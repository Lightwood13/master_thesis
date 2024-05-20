package org.kry.thesis.service.metrics

import org.kry.thesis.domain.Heater
import org.kry.thesis.domain.LatestMetrics
import org.kry.thesis.repository.LatestMetricsRepository
import org.kry.thesis.service.HeaterService
import org.kry.thesis.service.influxdb.InfluxDBService
import org.kry.thesis.service.mqtt.MQTTMessage
import org.kry.thesis.service.mqtt.MQTTQueueService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class MetricsService(
    mqttQueueService: MQTTQueueService,
    private val heaterService: HeaterService,
    private val latestMetricsRepository: LatestMetricsRepository,
    private val influxDBService: InfluxDBService
) {

    init {
        mqttQueueService.subscribe("/panel/metrics/#", ::processMetrics)
    }

    private fun processMetrics(metricsMessage: MQTTMessage) {
        val serial = parseSerialFromTopic(metricsMessage.topic)
        val metrics = metricsMessage.message
        println("Received metrics $metrics for serial $serial")

        val heater = heaterService.findBySerial(serial)
        val latestMetrics = latestMetricsRepository.findLatestMetricsByHeater(heater)
            ?: LatestMetrics(heater = heater)

        latestMetrics.metrics = metrics
        latestMetricsRepository.save(latestMetrics)

        influxDBService.saveMetrics(serial, metrics)
    }

    fun findLatestMetrics(heater: Heater): Map<HeaterMetric, Float> =
        latestMetricsRepository.findLatestMetricsByHeater(heater)?.metrics?.let { parseMetrics(it) } ?: emptyMap()

    private fun parseMetrics(metrics: String): Map<HeaterMetric, Float> =
        metrics.split(' ').firstOrNull()
            ?.split(',')?.mapNotNull { metric ->
                val code = metric.substringBefore('=', "").let { HeaterMetric.fromCode(it) } ?: return@mapNotNull null
                val value = metric.substringAfter('=', "").toFloatOrNull() ?: return@mapNotNull null
                code to value
            }?.toMap() ?: emptyMap()

    private fun parseSerialFromTopic(topic: String): String = topic.split("/").last()
}

enum class HeaterMetric(val code: String) {
    ROOM_TEMPERATURE("room_t");

    companion object {
        fun fromCode(code: String): HeaterMetric? =
            values().find { it.code == code }
    }
}
