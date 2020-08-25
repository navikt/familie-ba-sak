package no.nav.familie.ba.sak.kafka

import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import java.io.IOException

@Service
class Consumer {

    private val logger = LoggerFactory.getLogger(Producer::class.java)
    @KafkaListener(topics = ["aapen-barnetrygd-vedtak-v1"], groupId = "group_id") @Throws(IOException::class) fun consume(message: String?) {
        logger.info(String.format("#### -> Consumed message -> %s", message))
    }
}