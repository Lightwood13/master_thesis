package org.kry.thesis.service.influxdb

import com.influxdb.client.InfluxDBClient
import com.influxdb.client.domain.WritePrecision
import com.influxdb.query.FluxRecord
import org.kry.thesis.domain.Heater
import org.kry.thesis.service.StatisticsAggregationPeriod
import org.kry.thesis.service.StatisticsDataPoint
import org.kry.thesis.service.StatisticsField
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.ZoneId

@Service
class InfluxDBService(
    influxDBClient: InfluxDBClient
) {
    private val writeApi = influxDBClient.writeApiBlocking
    private val queryApi = influxDBClient.queryApi

    private val log = LoggerFactory.getLogger(InfluxDBService::class.java)

    fun saveMetrics(serial: String, metricsWithTimestamp: String) {
        writeApi.writeRecord(
            WritePrecision.MS,
            assembleInfluxDBMeasurement(serial, metricsWithTimestamp)
        )
    }

    fun assembleInfluxDBMeasurement(serial: String, metricsWithTimestamp: String): String =
        "$HEATER_SENSORS_MEASUREMENT,serial=$serial $metricsWithTimestamp"

    fun calculateLastWeekConsumption(target: Heater): Float? =
        safeInfluxDBQuery(
            fieldAggregationQuery(
                interval = "1w",
                serial = target.serial,
                measurement = HEATER_SENSORS_MEASUREMENT,
                field = StatisticsField.ELECTRIC_CONSUMPTION,
                aggregationFunction = InfluxDBAggregationFunction.SPREAD
            )
        ).firstOrNull()?.let { it.tryExtractSerialAndValue()?.second }

    fun calculateStatistics(
        target: Heater,
        field: StatisticsField,
        startTime: Instant,
        endTime: Instant,
        aggregationPeriod: StatisticsAggregationPeriod,
        timeZone: ZoneId?
    ): List<StatisticsDataPoint> {
        val query = windowAggregationQuery(
            startTime = startTime,
            endTime = endTime,
            serial = target.serial,
            measurement = HEATER_SENSORS_MEASUREMENT,
            field = field.toInfluxFieldName(),
            windowPeriod = aggregationPeriod.toInfluxPeriod(),
            windowOffset = aggregationPeriod.influxWindowOffset(),
            aggregationFunction = field.aggregationFunction(),
            timeZone = timeZone
        )

        return safeInfluxDBQuery(query)
            .mapNotNull { it.tryExtractStatisticsDataPoint() }
            .sortedBy { it.periodStart }
    }

    private fun safeInfluxDBQuery(query: String): List<FluxRecord> = try {
        queryApi.query(query)
            .flatMap { it.records }
    } catch (e: Exception) {
        log.error("Error executing InfluxDB query:\n{}", query)
        log.error(e.stackTraceToString())
        emptyList()
    }
}

private fun fieldAggregationQuery(
    interval: String,
    serial: String,
    measurement: String,
    field: StatisticsField,
    aggregationFunction: InfluxDBAggregationFunction
): String = """
        from(bucket: "$BUCKET")
            |> range(start: -$interval)
            |> filter(fn: (r) => r._measurement == "$measurement" and r._field == "${field.toInfluxFieldName()}")
            |> filter(fn: (r) => r.serial == "$serial")
            |> group(columns: ["serial"])
            |> ${aggregationFunction.value}()
            |> group()
""".trimIndent()

private fun windowAggregationQuery(
    startTime: Instant,
    endTime: Instant,
    serial: String,
    measurement: String,
    field: String,
    windowPeriod: String,
    windowOffset: String?,
    aggregationFunction: InfluxDBAggregationFunction,
    timeZone: ZoneId?
): String {
    val windowOffsetParameter = when {
        windowOffset != null -> ", offset: $windowOffset"
        else -> ""
    }
    val query = """
        from(bucket: "$BUCKET")
            |> range(start: $startTime, stop: $endTime)
            |> filter(fn: (r) => r._measurement == "$measurement" and r._field == "$field")
            |> filter(fn: (r) => r.serial == "$serial")
            |> window(every: $windowPeriod$windowOffsetParameter)
            |> ${aggregationFunction.value}()
            |> group(columns: ["serial", "_start", "_stop"])
            |> ${aggregationFunction.value}()
            |> group()
    """.trimIndent()

    return if (timeZone != null) {
        """
            import "timezone"
            option location = timezone.location(name: "${timeZone.id}")
        """.trimIndent() + "\n" + query
    } else {
        query
    }
}

private fun FluxRecord.tryExtractStatisticsDataPoint(): StatisticsDataPoint? {
    val startTime = values["_start"] as? Instant ?: return null
    val endTime = values["_stop"] as? Instant ?: return null
    val value = (this.value as? Double)?.toFloat() ?: return null
    return StatisticsDataPoint(
        periodStart = startTime,
        periodEnd = endTime,
        data = value
    )
}

private fun FluxRecord.tryExtractSerialAndValue(): Pair<String, Float>? {
    val serial = values["serial"] as? String ?: return null
    val value = (this.value as? Double)?.toFloat() ?: return null
    return serial to value
}

private enum class InfluxDBAggregationFunction(val value: String) {
    MEAN("mean"),
    SPREAD("spread")
}

private fun StatisticsField.toInfluxFieldName(): String =
    when (this) {
        StatisticsField.ELECTRIC_CONSUMPTION -> ELECTRIC_CONSUMPTION_FIELD
        StatisticsField.ROOM_TEMPERATURE -> ROOM_TEMPERATURE_FIELD
    }

private fun StatisticsField.aggregationFunction(): InfluxDBAggregationFunction =
    when (this) {
        StatisticsField.ELECTRIC_CONSUMPTION -> InfluxDBAggregationFunction.SPREAD
        StatisticsField.ROOM_TEMPERATURE -> InfluxDBAggregationFunction.MEAN
    }

private fun StatisticsAggregationPeriod.toInfluxPeriod(): String =
    when (this) {
        StatisticsAggregationPeriod.HOUR -> "1h"
        StatisticsAggregationPeriod.DAY -> "1d"
        StatisticsAggregationPeriod.WEEK -> "1w"
        StatisticsAggregationPeriod.MONTH -> "1mo"
    }

private fun StatisticsAggregationPeriod.influxWindowOffset(): String? =
    when (this) {
        StatisticsAggregationPeriod.WEEK -> "4d" // take into account that weeks in InfluxDB start from Thursday (Unix epoch)
        else -> null
    }

private const val BUCKET = "thesis"
private const val HEATER_SENSORS_MEASUREMENT = "heater-sensors"

private const val ELECTRIC_CONSUMPTION_FIELD = "el_co"
private const val ROOM_TEMPERATURE_FIELD = "room_t"
