package org.kry.thesis.service.metrics

import org.kry.thesis.service.influxdb.InfluxDBService
import org.kry.thesis.service.mqtt.MQTTMessage
import org.kry.thesis.service.mqtt.MQTTQueueService
import org.springframework.stereotype.Service

@Service
class MetricsService(
    mqttQueueService: MQTTQueueService,
    private val influxDBService: InfluxDBService
) {

    init {
        mqttQueueService.subscribe("/panel/metrics/#", ::processMetrics)
    }

    private fun processMetrics(metricsMessage: MQTTMessage) {
        val serial = parseSerialFromTopic(metricsMessage.topic)
        println("Received metrics ${metricsMessage.message} for serial $serial")
        influxDBService.saveMetrics(serial, metricsMessage.message)
    }

    private fun parseSerialFromTopic(topic: String): String = topic.split("/").last()
}
