package org.kry.thesis.service.facade

import org.kry.thesis.domain.*
import org.kry.thesis.service.*
import org.kry.thesis.service.dto.AdminUserDTO
import org.kry.thesis.service.metrics.*
import org.kry.thesis.service.metrics.HeaterMetric.*
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.http.HttpStatus
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.*
import org.springframework.transaction.event.*
import org.springframework.web.server.ResponseStatusException
import java.time.*

@Component
@Transactional
class HeaterFacade(
    private val heaterService: HeaterService,
    private val userService: UserService,
    private val metricsService: MetricsService,
    private val statisticsService: StatisticsService,
    private val websocketPublisher: SimpMessagingTemplate,
    private val modelService: ModelService,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val passwordEncoder: BCryptPasswordEncoder,
    private val locationService: LocationService,
    private val countryService: CountryService,
    private val kafkaService: KafkaService,
    private val scheduleService: ScheduleService
) {
    fun getCurrentUserHeaters(): List<Heater> {
        val user = userService.getCurrentUser()
        return heaterService.findByOwnerId(user.id!!)
    }

    fun getBySerialForCurrentUser(serial: String): HeaterDTO {
        val user = userService.getCurrentUser()
        val heater = heaterService.findBySerial(serial)
        if (heater.owner != user) {
            throw HeaterAccessForbiddenException()
        }

        return heater.toHeaterDTO()
    }

    fun getCurrentHeater(): HeaterDTO =
        heaterService.findCurrentHeater().toHeaterDTO()

    private fun Heater.toHeaterDTO(): HeaterDTO {
        val metrics = metricsService.findLatestMetrics(this)?.metrics?.let { parseMetrics(it) } ?: emptyMap()
        val lastWeekConsumption = statisticsService.calculateLastWeekConsumption(this)

        return HeaterDTO(
            id = this.id!!,
            name = this.name,
            serial = this.serial,
            roomTemperature = metrics[ROOM_TEMPERATURE],
            outsideTemperature = metrics[OUTSIDE_TEMPERATURE],
            heaterPower = this.power,
            weekConsumption = lastWeekConsumption,
            operationType = this.operationType,
            calibrationStatus = this.calibrationStatus,
            calibrationStart = this.calibrationStart,
            calibrationEnd = this.calibrationEnd,
            calibrationPercentage = this.calibrationPercentage,
            activeModelId = this.activeModel?.id,
            savings = metrics[SAVINGS],
            country = this.location?.country?.name,
            latitude = this.location?.latitude,
            longitude = this.location?.longitude
        )
    }

    fun createHeater(newHeaterDTO: NewHeaterDTO) {
        val encodedPassword = passwordEncoder.encode(newHeaterDTO.password)
        val user = userService.createUser(
            AdminUserDTO(
                login = newHeaterDTO.serial,
                authorities = mutableSetOf("ROLE_HEATER")
            )
        )

        user.password = encodedPassword
        heaterService.createHeater(
            Heater(
                name = newHeaterDTO.serial,
                serial = newHeaterDTO.serial,
                power = newHeaterDTO.power,
                heaterUser = user
            )
        )
    }

    fun addHeaterToCurrentUser(addHeaterDTO: AddHeaterDTO) {
        val user = userService.getCurrentUser()
        val heater = heaterService.findBySerial(addHeaterDTO.serial)

        if (!passwordEncoder.matches(addHeaterDTO.password, heater.heaterUser.password)) {
            throw WrongHeaterPasswordException()
        }

        val country = countryService.findById(addHeaterDTO.location.country_id)

        val location = locationService.createLocation(
            Location(
                latitude = addHeaterDTO.location.latitude,
                longitude = addHeaterDTO.location.longitude,
                country = country
            )
        )

        heater.name = addHeaterDTO.name
        heater.owner = user
        heater.location = location
    }

    fun getScheduleForCurrentHeater(date: LocalDate): Schedule? {
        val heater = heaterService.findCurrentHeater()
        return scheduleService.findByHeaterAndScheduleDate(heater, date)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun publishHeaterUpdate(heaterUpdatedEvent: HeaterUpdatedEvent) {
        val heater = heaterService.findBySerial(heaterUpdatedEvent.serial)

        websocketPublisher.convertAndSendToUser(
            heater.owner!!.login!!,
            "/queue/heater",
            "\"${heaterUpdatedEvent.serial}\""
        )
    }

    fun startCalibration(serial: String) {
        val heater = heaterService.findBySerial(serial)
        heater.calibrationStatus = CalibrationStatus.CALIBRATION_IN_PROGRESS
        heater.calibrationStart = metricsService.findLatestMetrics(heater)!!.timestamp
        heater.calibrationEnd = heater.calibrationStart!! + Duration.ofHours(78)
        heater.calibrationPercentage = 0f
        heater.operationType = OperationType.CALIBRATING

        publishHeaterUpdate(HeaterUpdatedEvent(heater.serial))
    }

    @EventListener
    fun onMetricReceived(event: MetricReceivedEvent) {
        val heater = heaterService.findBySerial(event.serial)

        updateCalibrationStatus(heater, event.lastMetricTimestamp)
        updateSchedules(heater, event.lastMetricTimestamp)
    }

    fun updateCalibrationStatus(heater: Heater, lastMetricTimestamp: Instant) {
        if (heater.calibrationStatus == CalibrationStatus.CALIBRATION_IN_PROGRESS) {
            if (lastMetricTimestamp > heater.calibrationEnd!!) {
                heater.calibrationStatus = CalibrationStatus.CALIBRATED
                heater.calibrationPercentage = 100f
                heater.operationType = OperationType.IDLE
                return
            }

            heater.calibrationPercentage =
                Duration.between(heater.calibrationStart!!, lastMetricTimestamp).seconds.toFloat() /
                Duration.between(heater.calibrationStart!!, heater.calibrationEnd!!).seconds
        }
    }

    fun updateSchedules(heater: Heater, lastMetricTimestamp: Instant) {
        if (heater.operationType != OperationType.MODEL) {
            return
        }

        val locationLastUpdated = heater.location?.lastUpdated
        val countryLastUpdated = heater.location?.country?.lastUpdated
        if (locationLastUpdated != null && countryLastUpdated != null) {
            val dataAvailableUntil = maxOf(locationLastUpdated, countryLastUpdated)
            val lastValidScheduleDate = heater.lastValidScheduleDate
            if (lastValidScheduleDate == null || lastValidScheduleDate < dataAvailableUntil) {
                val currentDate =
                    lastMetricTimestamp.atZone(ZoneId.of(heater.location?.country?.timezone)).toLocalDate()

                var date = if (lastValidScheduleDate == null) {
                    currentDate
                } else {
                    maxOf(currentDate, lastValidScheduleDate.plusDays(1))
                }

                while (date <= dataAvailableUntil) {
                    calculateSchedule(heater.activeModel!!, date, heater.location!!.country.timezone)
                    date = date.plusDays(1)
                }
            }
        }
    }

    fun calculateSchedule(model: Model, date: LocalDate, timezone: String) {
        if (model.status != ModelStatus.Trained) {
            return
        }

        kafkaService.sendMLServiceCommand(
            CalculateScheduleCommand(
                modelId = model.id!!,
                date = date,
                timezone = timezone
            )
        )
    }

    fun getHeaterModels(serial: String): List<Model> =
        heaterService.findBySerial(serial).models

    fun createModel(serial: String, newModelDTO: NewModelDTO) {
        val heater = heaterService.findBySerial(serial)
        val latestMetrics = metricsService.findLatestMetrics(heater)
        val model = modelService.createNewModel(heater, newModelDTO, latestMetrics?.timestamp ?: Instant.now())
        if (newModelDTO.activateImmediately) {
            heater.activeModel = model
        }

        kafkaService.sendMLServiceCommand(
            CreateModelCommand(
                modelId = model.id!!,
                serial = heater.serial,
                targetTemperature = newModelDTO.targetTemperature,
                minTemperature = newModelDTO.minTemperature,
                maxTemperature = newModelDTO.maxTemperature,
                calibrationDataStart = heater.calibrationStart!!,
                calibrationDataEnd = heater.calibrationEnd!!
            )
        )

        applicationEventPublisher.publishEvent(HeaterUpdatedEvent(serial))
    }

    @EventListener
    fun modelTrainingFinished(event: ModelTrainingFinishedEvent) {
        val model = modelService.findById(event.modelId)

        if (model.heater.activeModel == model) {
            applicationEventPublisher.publishEvent(HeaterUpdatedEvent(model.heater.serial))
            model.heater.operationType = OperationType.MODEL
        }

        model.status = ModelStatus.Trained
    }

    @EventListener
    fun scheduleCalculated(event: ScheduleCalculatedEvent) {
        val model = modelService.findById(event.modelId)
        val heater = model.heater

        if (heater.activeModel != model) {
            return
        }

        val schedule = scheduleService.findByHeaterAndScheduleDate(heater, event.date)
            ?: Schedule(heater = heater, scheduleDate = event.date, data = event.schedule)
        schedule.data = event.schedule
        val lastValidScheduleDate = heater.lastValidScheduleDate
        heater.lastValidScheduleDate = if (lastValidScheduleDate == null) {
            schedule.scheduleDate
        } else {
            maxOf(lastValidScheduleDate, schedule.scheduleDate)
        }

        scheduleService.save(schedule)
    }
}

data class HeaterDTO(
    val id: Long,
    val name: String,
    val serial: String,
    val roomTemperature: Float?,
    val outsideTemperature: Float?,
    val heaterPower: Float,
    val weekConsumption: Float?,
    val operationType: OperationType,
    val calibrationStatus: CalibrationStatus,
    val calibrationStart: Instant?,
    val calibrationEnd: Instant?,
    val calibrationPercentage: Float?,
    val activeModelId: Long?,
    val savings: Float?,
    val country: String?,
    val latitude: Float?,
    val longitude: Float?
)

data class NewHeaterDTO(
    val serial: String,
    val password: String,
    val power: Float
)

data class AddHeaterDTO(
    val name: String,
    val serial: String,
    val password: String,
    val location: LocationDTO
)

data class LocationDTO(
    val latitude: Float,
    val longitude: Float,
    val country_id: Long
)

data class NewModelDTO(
    val name: String,
    val targetTemperature: Float,
    val minTemperature: Float,
    val maxTemperature: Float,
    val activateImmediately: Boolean
)

data class HeaterUpdatedEvent(val serial: String)

data class MetricReceivedEvent(
    val serial: String,
    val lastMetricTimestamp: Instant
)

data class ModelTrainingFinishedEvent(val modelId: Long)

data class ScheduleCalculatedEvent(
    val modelId: Long,
    val date: LocalDate,
    val schedule: String
)

class WrongHeaterPasswordException : ResponseStatusException(HttpStatus.UNAUTHORIZED, "Heater password is incorrect")
