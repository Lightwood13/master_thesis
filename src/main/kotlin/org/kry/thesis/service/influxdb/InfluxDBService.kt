package org.kry.thesis.service.influxdb

import com.influxdb.client.InfluxDBClient
import com.influxdb.client.write.WriteParameters
import org.springframework.stereotype.Service

@Service
class InfluxDBService(
    private val influxDBClient: InfluxDBClient
) {
    private val writeApi = influxDBClient.writeApiBlocking

    fun saveMetrics(serial: String, metricsWithTimestamp: String) {
        writeApi.writeRecord(
            WriteParameters.DEFAULT_WRITE_PRECISION,
            assembleInfluxDBMeasurement(serial, metricsWithTimestamp)
        )
    }

    fun assembleInfluxDBMeasurement(serial: String, metricsWithTimestamp: String): String =
        "$MEASUREMENTS,serial=$serial $metricsWithTimestamp"

    companion object {
        private const val MEASUREMENTS = "heating-panel-sensors"
    }
}
