package org.kry.thesis.service.influxdb

import com.influxdb.client.InfluxDBClient
import com.influxdb.client.domain.WritePrecision
import com.influxdb.query.FluxRecord
import org.kry.thesis.domain.Country
import org.kry.thesis.domain.Heater
import org.kry.thesis.domain.Location
import org.kry.thesis.service.StatisticsAggregationPeriod
import org.kry.thesis.service.StatisticsDataPoint
import org.kry.thesis.service.StatisticsField
import org.kry.thesis.service.StatisticsField.*
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
            assembleHeaterSensorsMeasurement(serial, metricsWithTimestamp)
        )
    }

    fun saveOutsideTemperature(location: Location, temperatures: List<Float>, timestamps: List<Instant>) {
        assert(temperatures.size == timestamps.size)
        temperatures.zip(timestamps).map { (temperature, timestamp) ->
            writeApi.writeRecord(
                WritePrecision.MS,
                assembleOutsideTemperatureMeasurement(location.id!!, temperature, timestamp)
            )
        }
    }

    fun savePrices(country: Country, prices: List<Float>, timestamps: List<Instant>) {
        assert(prices.size == timestamps.size)
        prices.zip(timestamps).map { (price, timestamp) ->
            writeApi.writeRecord(
                WritePrecision.MS,
                assemblePriceMeasurement(country.id!!, price, timestamp)
            )
        }
    }

    private fun assembleHeaterSensorsMeasurement(serial: String, metricsWithTimestamp: String): String =
        "$HEATER_SENSORS_MEASUREMENT,serial=$serial $metricsWithTimestamp"

    private fun assembleOutsideTemperatureMeasurement(locationId: Long, temperature: Float, timestamp: Instant): String =
        "$OUTSIDE_TEMPERATURE_MEASUREMENT,loc_id=$locationId temp=$temperature ${timestamp.toEpochMilli()}"

    private fun assemblePriceMeasurement(countryId: Long, price: Float, timestamp: Instant): String =
        "$PRICE_MEASUREMENT,country_id=$countryId price=$price ${timestamp.toEpochMilli()}"

    fun calculateLastWeekConsumption(target: Heater): Float? =
        safeInfluxDBQuery(
            fieldAggregationQuery(
                interval = "1w",
                serial = target.serial,
                measurement = HEATER_SENSORS_MEASUREMENT,
                field = ELECTRIC_CONSUMPTION,
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
            idField = field.idField(),
            idValue = field.idValue(target),
            measurement = field.toMeasurement(),
            field = field.toInfluxFieldName(),
            windowPeriod = aggregationPeriod.toInfluxPeriod(),
            windowOffset = aggregationPeriod.influxWindowOffset(),
            aggregationFunction = field.aggregationFunction(),
            endOperation = if (field == ELECTRIC_CONSUMPTION) {
                "difference"
            } else {
                null
            },
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
    idField: String,
    idValue: String,
    measurement: String,
    field: String,
    windowPeriod: String,
    windowOffset: String?,
    aggregationFunction: InfluxDBAggregationFunction,
    endOperation: String?,
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
            |> filter(fn: (r) => r.$idField == "$idValue")
            |> window(every: $windowPeriod$windowOffsetParameter)
            |> ${aggregationFunction.value}()
            |> group()
            ${if (endOperation != null) "|> $endOperation()" else ""}
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
    MIN("min"),
    MEAN("mean"),
    SPREAD("spread")
}

private fun StatisticsField.toMeasurement(): String =
    when (this) {
        ELECTRIC_CONSUMPTION, ROOM_TEMPERATURE -> HEATER_SENSORS_MEASUREMENT
        OUTSIDE_TEMPERATURE -> OUTSIDE_TEMPERATURE_MEASUREMENT
        ELECTRICITY_PRICE -> PRICE_MEASUREMENT
    }

private fun StatisticsField.idField(): String =
    when (this) {
        ELECTRIC_CONSUMPTION, ROOM_TEMPERATURE -> "serial"
        OUTSIDE_TEMPERATURE -> "loc_id"
        ELECTRICITY_PRICE -> "country_id"
    }

private fun StatisticsField.idValue(heater: Heater): String =
    when (this) {
        ELECTRIC_CONSUMPTION, ROOM_TEMPERATURE -> heater.serial
        OUTSIDE_TEMPERATURE -> heater.location?.id?.toString() ?: "unknown"
        ELECTRICITY_PRICE -> heater.location?.country?.id?.toString() ?: "unknown"
    }

private fun StatisticsField.toInfluxFieldName(): String =
    when (this) {
        ELECTRIC_CONSUMPTION -> ELECTRIC_CONSUMPTION_FIELD
        ROOM_TEMPERATURE -> ROOM_TEMPERATURE_FIELD
        OUTSIDE_TEMPERATURE -> OUTSIDE_TEMPERATURE_FIELD
        ELECTRICITY_PRICE -> PRICE_FIELD
    }

private fun StatisticsField.aggregationFunction(): InfluxDBAggregationFunction =
    when (this) {
        ELECTRIC_CONSUMPTION -> InfluxDBAggregationFunction.MIN
        ROOM_TEMPERATURE, OUTSIDE_TEMPERATURE, ELECTRICITY_PRICE -> InfluxDBAggregationFunction.MEAN
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
private const val OUTSIDE_TEMPERATURE_MEASUREMENT = "out-temp"
private const val PRICE_MEASUREMENT = "price"

private const val ELECTRIC_CONSUMPTION_FIELD = "el_co"
private const val ROOM_TEMPERATURE_FIELD = "room_t"
private const val OUTSIDE_TEMPERATURE_FIELD = "temp"
private const val PRICE_FIELD = "price"
