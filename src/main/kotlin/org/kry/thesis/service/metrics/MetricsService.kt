package org.kry.thesis.service.metrics

import org.kry.thesis.domain.Heater
import org.kry.thesis.domain.LatestMetrics
import org.kry.thesis.repository.LatestMetricsRepository
import org.kry.thesis.service.HeaterService
import org.kry.thesis.service.facade.HeaterUpdatedEvent
import org.kry.thesis.service.facade.MetricReceivedEvent
import org.kry.thesis.service.influxdb.InfluxDBService
import org.kry.thesis.service.mqtt.MQTTMessage
import org.kry.thesis.service.mqtt.MQTTQueueService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional
class MetricsService(
    mqttQueueService: MQTTQueueService,
    private val heaterService: HeaterService,
    private val latestMetricsRepository: LatestMetricsRepository,
    private val influxDBService: InfluxDBService,
    private val applicationEventPublisher: ApplicationEventPublisher
) {

    init {
        mqttQueueService.subscribe("/panel/metrics/#") {
            applicationEventPublisher.publishEvent(it)
        }
    }

    @EventListener
    fun processMetrics(metricsMessage: MQTTMessage) {
        val serial = parseSerialFromTopic(metricsMessage.topic)
        val metricsWithTimestamp = metricsMessage.message
        val metrics = metricsWithTimestamp.split(' ').first()
        val timestamp = parseMetricTimestamp(metricsWithTimestamp)!!
        println("Received metrics $metrics for serial $serial")

        val heater = heaterService.findBySerial(serial)
        val latestMetrics = latestMetricsRepository.findLatestMetricsByHeater(heater)
            ?: LatestMetrics(heater = heater, metrics = metrics, timestamp = timestamp)

        latestMetrics.metrics = metrics
        latestMetrics.timestamp = timestamp
        latestMetricsRepository.save(latestMetrics)

        influxDBService.saveMetrics(serial, metricsWithTimestamp)

        applicationEventPublisher.publishEvent(MetricReceivedEvent(serial = heater.serial, lastMetricTimestamp = timestamp))
        applicationEventPublisher.publishEvent(HeaterUpdatedEvent(heater.serial))
    }

    fun findLatestMetrics(heater: Heater): LatestMetrics? =
        latestMetricsRepository.findLatestMetricsByHeater(heater)

    private fun parseMetricTimestamp(metrics: String): Instant? =
        metrics.split(' ').lastOrNull()
            ?.toLongOrNull()
            ?.let { Instant.ofEpochMilli(it) }

    private fun parseSerialFromTopic(topic: String): String = topic.split("/").last()
}

fun parseMetrics(metrics: String): Map<HeaterMetric, Float> =
    metrics.split(' ').firstOrNull()
        ?.split(',')?.mapNotNull { metric ->
            val code = metric.substringBefore('=', "").let { HeaterMetric.fromCode(it) } ?: return@mapNotNull null
            val value = metric.substringAfter('=', "").toFloatOrNull() ?: return@mapNotNull null
            code to value
        }?.toMap() ?: emptyMap()

enum class HeaterMetric(val code: String) {
    ROOM_TEMPERATURE("room_t"),
    OUTSIDE_TEMPERATURE("out_t"),
    SAVINGS("sav");

    companion object {
        fun fromCode(code: String): HeaterMetric? =
            values().find { it.code == code }
    }
}
