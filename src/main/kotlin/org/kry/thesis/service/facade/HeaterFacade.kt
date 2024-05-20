package org.kry.thesis.service.facade

import org.kry.thesis.domain.Heater
import org.kry.thesis.service.HeaterService
import org.kry.thesis.service.StatisticsService
import org.kry.thesis.service.UserService
import org.kry.thesis.service.metrics.HeaterMetric.ROOM_TEMPERATURE
import org.kry.thesis.service.metrics.MetricsService
import org.springframework.stereotype.Component

@Component
class HeaterFacade(
    private val heaterService: HeaterService,
    private val userService: UserService,
    private val metricsService: MetricsService,
    private val statisticsService: StatisticsService
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

        val metrics = metricsService.findLatestMetrics(heater)
        val lastWeekConsumption = statisticsService.calculateLastWeekConsumption(heater)

        return HeaterDTO(
            id = heater.id!!,
            serial = heater.serial,
            roomTemperature = metrics[ROOM_TEMPERATURE],
            outsideTemperature = null,
            heaterPower = heater.power,
            weekConsumption = lastWeekConsumption
        )
    }
}

data class HeaterDTO(
    val id: Long,
    val serial: String,
    val roomTemperature: Float?,
    val outsideTemperature: Float?,
    val heaterPower: Float,
    val weekConsumption: Float?
)
