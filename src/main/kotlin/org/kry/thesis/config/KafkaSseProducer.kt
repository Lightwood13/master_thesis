package org.kry.thesis.config

import org.springframework.cloud.stream.annotation.Output
import org.springframework.messaging.MessageChannel

interface KafkaSseProducer {

    companion object {
        const val ML_SERVICE_COMMAND_CHANNEL = "ml-service-command-topic"
    }

    @Output(ML_SERVICE_COMMAND_CHANNEL)
    fun commandOutput(): MessageChannel
}
