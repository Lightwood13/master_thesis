package org.kry.thesis.service.facade

import org.kry.thesis.domain.CalibrationStatus
import org.kry.thesis.domain.Heater
import org.kry.thesis.domain.Location
import org.kry.thesis.domain.Model
import org.kry.thesis.domain.ModelStatus
import org.kry.thesis.domain.Schedule
import org.kry.thesis.service.CountryService
import org.kry.thesis.service.HeaterService
import org.kry.thesis.service.LocationService
import org.kry.thesis.service.ModelService
import org.kry.thesis.service.StatisticsService
import org.kry.thesis.service.UserService
import org.kry.thesis.service.metrics.HeaterMetric.*
import org.kry.thesis.service.metrics.MetricsService
import org.kry.thesis.service.metrics.parseMetrics
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.http.HttpStatus
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.web.server.ResponseStatusException
import java.time.Duration
import java.time.Instant

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
    private val countryService: CountryService
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
            schedule = this.schedule,
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
        heaterService.createHeater(
            Heater(
                name = newHeaterDTO.serial,
                serial = newHeaterDTO.serial,
                passwordHash = passwordEncoder.encode(newHeaterDTO.password),
                schedule = Schedule.IDLE,
                calibrationStatus = CalibrationStatus.NOT_CALIBRATED,
                power = newHeaterDTO.power,
            )
        )
    }

    fun addHeaterToCurrentUser(addHeaterDTO: AddHeaterDTO) {
        val user = userService.getCurrentUser()
        val heater = heaterService.findBySerial(addHeaterDTO.serial)

        if (!passwordEncoder.matches(addHeaterDTO.password, heater.passwordHash)) {
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
        heater.schedule = Schedule.CALIBRATING

        publishHeaterUpdate(HeaterUpdatedEvent(heater.serial))
    }

    @EventListener
    fun updateSchedule(event: UpdateScheduleEvent) {
        val heater = heaterService.findBySerial(event.serial)
        if (heater.calibrationStatus == CalibrationStatus.CALIBRATION_IN_PROGRESS) {
            if (event.lastMetricTimestamp > heater.calibrationEnd!!) {
                heater.calibrationStatus = CalibrationStatus.CALIBRATED
                heater.calibrationPercentage = 100f
                heater.schedule = Schedule.IDLE
                return
            }

            heater.calibrationPercentage =
                Duration.between(heater.calibrationStart!!, event.lastMetricTimestamp).seconds.toFloat() /
                Duration.between(heater.calibrationStart!!, heater.calibrationEnd!!).seconds
        } else if (heater.calibrationStatus == CalibrationStatus.CALIBRATED) {
            if (heater.activeModel?.status == ModelStatus.Training &&
                Duration.between(heater.activeModel!!.createdOn, event.lastMetricTimestamp) >= Duration.ofHours(10)
            ) {
                heater.activeModel!!.status = ModelStatus.Working
                heater.schedule = Schedule.MODEL
            }
        }
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
        applicationEventPublisher.publishEvent(HeaterUpdatedEvent(serial))
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
    val schedule: Schedule,
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

data class UpdateScheduleEvent(
    val serial: String,
    val lastMetricTimestamp: Instant
)

class WrongHeaterPasswordException : ResponseStatusException(HttpStatus.UNAUTHORIZED, "Heater password is incorrect")
