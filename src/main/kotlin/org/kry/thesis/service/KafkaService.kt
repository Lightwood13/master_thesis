package org.kry.thesis.service

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.kry.thesis.config.*
import org.kry.thesis.service.facade.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.context.ApplicationEventPublisher
import org.springframework.messaging.*
import org.springframework.messaging.support.GenericMessage
import org.springframework.stereotype.Service
import org.springframework.util.MimeTypeUtils
import java.time.*

@Service
class KafkaService(
    @Qualifier(KafkaSseProducer.ML_SERVICE_COMMAND_CHANNEL) private val mlServiceCommandChannel: MessageChannel,
    private val objectMapper: ObjectMapper,
    private val applicationEventPublisher: ApplicationEventPublisher
) {
    fun sendMLServiceCommand(command: MLServiceCommand) {
        val headers = MessageHeaders(
            mapOf(MessageHeaders.CONTENT_TYPE to MimeTypeUtils.TEXT_PLAIN_VALUE)
        )
        mlServiceCommandChannel.send(
            GenericMessage(
                objectMapper.writeValueAsString(command),
                headers
            )
        )
    }

    @StreamListener(value = KafkaSseConsumer.ML_SERVICE_RESPONSE_CHANNEL, copyHeaders = "false")
    fun consume(message: Message<String>) {
        val response = objectMapper.readValue(message.payload, MLServiceResponse::class.java)
        when (response) {
            is ModelTrainingFinishedResponse -> {
                applicationEventPublisher.publishEvent(ModelTrainingFinishedEvent(modelId = response.modelId))
            }

            is ScheduleCalculatedResponse -> {
                applicationEventPublisher.publishEvent(
                    ScheduleCalculatedEvent(
                        modelId = response.modelId,
                        date = response.date,
                        schedule = objectMapper.writeValueAsString(response.schedule)
                    )
                )
            }
        }
    }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    value = [
        JsonSubTypes.Type(value = CreateModelCommand::class, name = "CREATE_MODEL"),
        JsonSubTypes.Type(value = CalculateScheduleCommand::class, name = "CALCULATE_SCHEDULE")
    ]
)
sealed class MLServiceCommand

data class CreateModelCommand(
    val modelId: Long,
    val serial: String,
    val targetTemperature: Float,
    val minTemperature: Float,
    val maxTemperature: Float,
    val calibrationDataStart: Instant,
    val calibrationDataEnd: Instant
) : MLServiceCommand()

data class CalculateScheduleCommand(
    val modelId: Long,
    val date: LocalDate,
    val timezone: String
) : MLServiceCommand()

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    value = [
        JsonSubTypes.Type(value = ModelTrainingFinishedResponse::class, name = "MODEL_TRAINING_FINISHED"),
        JsonSubTypes.Type(value = ScheduleCalculatedResponse::class, name = "SCHEDULE_CALCULATED")
    ]
)
sealed class MLServiceResponse

data class ModelTrainingFinishedResponse(
    val modelId: Long
) : MLServiceResponse()

data class ScheduleCalculatedResponse(
    val modelId: Long,
    val date: LocalDate,
    val schedule: List<Float>
) : MLServiceResponse()
