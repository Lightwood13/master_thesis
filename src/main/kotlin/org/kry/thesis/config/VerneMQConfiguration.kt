package org.kry.thesis.config

import org.eclipse.paho.client.mqttv3.IMqttClient
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.MqttSecurityException
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.kry.thesis.service.mqtt.MQTTMessage
import org.kry.thesis.service.mqtt.MQTTQueueService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.*

@Configuration
class VerneMQConfiguration(
    @Value("\${verneMQ.address}") private val address: String,
    private val mqttQueueService: MQTTQueueService
) : MqttCallbackExtended {

    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun getVerneMQ(): IMqttClient = MqttClient("tcp://$address", UUID.randomUUID().toString(), MemoryPersistence())
        .also {
            try {
                it.connect(
                    MqttConnectOptions().apply {
                        isCleanSession = true
                        connectionTimeout = 10 // seconds
                        isAutomaticReconnect = true
                        maxReconnectDelay = 10000 // millis
                    }
                )
                it.setCallback(this)
                subscribeToPanelMetrics(it)
            } catch (e: MqttSecurityException) {
                log.error("Default user not found, ${e.message}")
            }
        }

    private fun subscribeToPanelMetrics(client: IMqttClient) {
        client.subscribe(arrayOf("$PANEL_METRICS_PREFIX/#"))
    }

    override fun connectionLost(cause: Throwable?) {
        log.error("Connection lost because: $cause")
    }

    override fun messageArrived(topic: String?, message: MqttMessage?) {
        if (topic?.startsWith("$PANEL_METRICS_PREFIX/") == true) {
            mqttQueueService.dispatchMQTTMessage(MQTTMessage(topic, message.toString()))
        }
    }

    override fun deliveryComplete(token: IMqttDeliveryToken?) {}

    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
        try {
            subscribeToPanelMetrics(getVerneMQ())
        } catch (e: MqttException) {
            log.error(e.message)
        }
    }

    companion object {
        private const val PANEL_METRICS_PREFIX = "/panel/metrics"
    }
}
