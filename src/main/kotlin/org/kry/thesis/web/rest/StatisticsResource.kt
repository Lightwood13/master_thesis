package org.kry.thesis.web.rest

import org.kry.thesis.service.StatisticsAggregationPeriod
import org.kry.thesis.service.StatisticsField
import org.kry.thesis.service.StatisticsWindowData
import org.kry.thesis.service.facade.StatisticsFacade
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/statistics")
class StatisticsResource(
    private val statisticsFacade: StatisticsFacade
) {

    @PostMapping("/calculate")
    fun calculateStatistics(
        @RequestBody statisticsRequestDTO: StatisticsRequestDTO
    ): List<StatisticsWindowData> =
        statisticsFacade.calculateStatistics(statisticsRequestDTO)
}

data class StatisticsRequestDTO(
    val serial: String,
    val fields: Set<StatisticsField>,
    val startTime: Instant,
    val endTime: Instant,
    val aggregationPeriod: StatisticsAggregationPeriod,
    val timeZone: String
)
