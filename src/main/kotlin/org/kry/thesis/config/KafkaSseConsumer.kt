package org.kry.thesis.config

import org.springframework.cloud.stream.annotation.Input
import org.springframework.messaging.MessageChannel

interface KafkaSseConsumer {

    companion object {
        const val ML_SERVICE_RESPONSE_CHANNEL = "ml-service-response-topic"
    }

    @Input(ML_SERVICE_RESPONSE_CHANNEL)
    fun input(): MessageChannel
}
