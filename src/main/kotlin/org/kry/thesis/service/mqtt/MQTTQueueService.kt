package org.kry.thesis.service.mqtt

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors

@Service
class MQTTQueueService {

    private val log = LoggerFactory.getLogger(javaClass)

    private val subscribers = ConcurrentLinkedQueue<MQTTSubscriber>()
    private val executorService = Executors.newFixedThreadPool(20)

    fun subscribe(topic: String, callback: (MQTTMessage) -> Unit) {
        subscribers.add(MQTTSubscriber(topic, callback))
    }

    fun dispatchMQTTMessage(message: MQTTMessage) {
        subscribers
            .filter {
                it.topic == message.topic || (
                    it.topic.contains("#") &&
                        message.topic.startsWith(it.topic.replace("/#", ""))
                    )
            }
            .forEach {
                executorService.submit {
                    try {
                        it.callback(message)
                    } catch (e: Exception) {
                        log.error(e.message)
                    }
                }
            }
    }
}

private data class MQTTSubscriber(val topic: String, val callback: (MQTTMessage) -> Unit)

data class MQTTMessage(val topic: String, val message: String)
