package org.kry.thesis.service

import org.kry.thesis.domain.Heater
import org.kry.thesis.service.influxdb.InfluxDBService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.*

@Service
class StatisticsService(
    private val influxDBService: InfluxDBService
) {
    private val log = LoggerFactory.getLogger(StatisticsService::class.java)

    fun calculateLastWeekConsumption(target: Heater): Float? =
        influxDBService.calculateLastWeekConsumption(target)

    fun calculateStatistics(
        target: Heater,
        fields: Set<StatisticsField>,
        startTime: Instant,
        endTime: Instant,
        aggregationPeriod: StatisticsAggregationPeriod,
        timeZone: ZoneId
    ): List<StatisticsWindowData> {
        val statisticsData: Map<StatisticsField, List<StatisticsDataPoint>> = fields.associateWith { field ->
            influxDBService.calculateStatistics(
                target,
                field,
                startTime,
                endTime,
                aggregationPeriod,
                timeZone
            )
        }
        return combineStatisticsData(
            data = statisticsData,
            startTime = startTime,
            endTime = endTime,
            aggregationPeriod = aggregationPeriod,
            timeZone = timeZone
        )
    }

    private fun combineStatisticsData(
        data: Map<StatisticsField, List<StatisticsDataPoint>>,
        startTime: Instant,
        endTime: Instant,
        aggregationPeriod: StatisticsAggregationPeriod,
        timeZone: ZoneId
    ): List<StatisticsWindowData> {
        val result = mutableListOf<StatisticsWindowData>()
        val fields: Set<StatisticsField> = data.keys

        val rangeStart: ZonedDateTime = startTime.atZone(timeZone)
        val rangeEnd: ZonedDateTime = endTime.atZone(timeZone)

        var currentWindowStart: ZonedDateTime = startTime.atZone(timeZone)
            .roundDownToAggregationPeriodStart(aggregationPeriod)
        var currentWindowEnd: ZonedDateTime = currentWindowStart.addAggregationPeriod(aggregationPeriod)

        while (
            rangesIntersect(
                currentWindowStart, currentWindowEnd,
                rangeStart, rangeEnd
            )
        ) {
            val currentWindowFieldData: Map<StatisticsField, Float?> = fields.associateWith { field ->
                val fieldData = data[field]

                val dataPoint: Float? = fieldData?.filterPointsIntersectingWithRange(
                    currentWindowStart.toInstant(),
                    currentWindowEnd.toInstant()
                )?.extractSinglePointOrLogError(
                    fieldData, timeZone, currentWindowStart, currentWindowEnd
                )?.data

                dataPoint
            }

            result += StatisticsWindowData(
                periodStart = currentWindowStart.coerceAtLeast(rangeStart).toInstant(),
                periodEnd = currentWindowEnd.coerceAtMost(rangeEnd).toInstant(),
                data = currentWindowFieldData
            )

            val newWindowEnd = currentWindowEnd.addAggregationPeriod(aggregationPeriod)
            currentWindowStart = currentWindowEnd
            currentWindowEnd = newWindowEnd
        }

        return result
    }

    private fun List<StatisticsDataPoint>.filterPointsIntersectingWithRange(
        start: Instant,
        end: Instant
    ): List<StatisticsDataPoint> = filter {
        rangesIntersect(
            start, end,
            it.periodStart, it.periodEnd
        )
    }

    private fun <T> List<T>.extractSinglePointOrLogError(
        statisticsData: List<StatisticsDataPoint>,
        timeZone: ZoneId,
        currentWindowStart: ZonedDateTime,
        currentWindowEnd: ZonedDateTime
    ): T? = firstOrNull().also {
        if (size > 1) {
            log.error(
                MisalignedIntervalsException(
                    statisticsData = statisticsData,
                    timeZone = timeZone,
                    currentWindowStart = currentWindowStart,
                    currentWindowEnd = currentWindowEnd
                ).toString()
            )
        }
    }

    private fun ZonedDateTime.roundDownToAggregationPeriodStart(period: StatisticsAggregationPeriod): ZonedDateTime =
        when (period) {
            StatisticsAggregationPeriod.HOUR -> ZonedDateTime.of(
                this.year,
                this.monthValue,
                this.dayOfMonth,
                this.hour,
                0,
                0,
                0,
                this.zone
            )

            StatisticsAggregationPeriod.DAY -> this.toLocalDate().atStartOfDay(this.zone)
            StatisticsAggregationPeriod.WEEK -> {
                val date = this.toLocalDate()
                date.minusDays(date.dayOfWeek.value.toLong() - 1).atStartOfDay(this.zone)
            }

            StatisticsAggregationPeriod.MONTH -> LocalDate.of(this.year, this.month, 1).atStartOfDay(this.zone)
        }

    private fun ZonedDateTime.addAggregationPeriod(period: StatisticsAggregationPeriod): ZonedDateTime =
        when (period) {
            StatisticsAggregationPeriod.HOUR -> this.plusHours(1)
            StatisticsAggregationPeriod.DAY -> this.plusDays(1)
            StatisticsAggregationPeriod.WEEK -> this.plusWeeks(1)
            StatisticsAggregationPeriod.MONTH -> this.plusMonths(1)
        }

    companion object {
        const val KWH_TO_J = 3_600_000
    }
}

enum class StatisticsField {
    ELECTRIC_CONSUMPTION,
    ROOM_TEMPERATURE,
    OUTSIDE_TEMPERATURE,
    ELECTRICITY_PRICE
}

enum class StatisticsAggregationPeriod {
    HOUR,
    DAY,
    WEEK,
    MONTH
}

data class StatisticsDataPoint(
    val periodStart: Instant,
    val periodEnd: Instant,
    val data: Float
)

data class StatisticsWindowData(
    val periodStart: Instant,
    val periodEnd: Instant,
    val data: Map<StatisticsField, Float?>
)

fun <T : Comparable<T>> rangesIntersect(
    firstStart: T,
    firstEndExclusive: T,
    secondStart: T,
    secondEndExclusive: T
): Boolean =
// no intersection:       <---second--->   <---first--->     or      <---first--->   <---second-->
// intersection:    not ( <---second--->   <---first---> ) and not ( <---first--->   <---second--> )
    !(firstStart >= secondEndExclusive) && !(secondStart >= firstEndExclusive)

class MisalignedIntervalsException(
    statisticsData: List<StatisticsDataPoint>,
    timeZone: ZoneId,
    currentWindowStart: ZonedDateTime,
    currentWindowEnd: ZonedDateTime
) : RuntimeException(
    """
    Misaligned statistics intervals: two InfluxDB intervals intersect one server interval.
    Time zone: $timeZone
    Statistics data: $statisticsData
    Current interval: $currentWindowStart - $currentWindowEnd
    """.trimIndent()
)
