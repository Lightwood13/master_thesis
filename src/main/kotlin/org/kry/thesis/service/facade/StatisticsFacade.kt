package org.kry.thesis.service.facade

import org.kry.thesis.service.HeaterService
import org.kry.thesis.service.StatisticsService
import org.kry.thesis.service.StatisticsWindowData
import org.kry.thesis.service.UserService
import org.kry.thesis.web.rest.StatisticsRequestDTO
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.ZoneId

@Component
@Transactional(readOnly = true)
class StatisticsFacade(
    private val heaterService: HeaterService,
    private val userService: UserService,
    private val statisticsService: StatisticsService
) {
    fun calculateStatistics(statisticsRequestDTO: StatisticsRequestDTO): List<StatisticsWindowData> {
        val heater = heaterService.findBySerial(statisticsRequestDTO.serial)
        val user = userService.getCurrentUser()
        if (heater.owner != user) {
            throw HeaterAccessForbiddenException()
        }

        val timeZone = try {
            ZoneId.of(statisticsRequestDTO.timeZone)
        } catch (e: Exception) {
            throw TimeZoneNotFoundException()
        }

        return statisticsService.calculateStatistics(
            target = heater,
            fields = statisticsRequestDTO.fields,
            startTime = statisticsRequestDTO.startTime,
            endTime = statisticsRequestDTO.endTime,
            aggregationPeriod = statisticsRequestDTO.aggregationPeriod,
            timeZone = timeZone
        )
    }
}

class HeaterAccessForbiddenException : ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have access to this heater")

class TimeZoneNotFoundException : ResponseStatusException(HttpStatus.NOT_FOUND, "Timezone not found")
