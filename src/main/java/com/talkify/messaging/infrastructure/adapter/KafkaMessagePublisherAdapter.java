package com.talkify.messaging.infrastructure.adapter;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.talkify.messaging.application.event.MessageDispatchEvent;
import com.talkify.messaging.application.port.MessageDispatchPort;
import com.talkify.messaging.infrastructure.kafka.KafkaConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaMessagePublisherAdapter implements MessageDispatchPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void dispatch(MessageDispatchEvent event) {
        kafkaTemplate.send(KafkaConfig.CHAT_TOPIC, event.conversationId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Kafka publish FAILED | msgId={} error={}",
                                event.messageId(), ex.getMessage());
                    } else {
                        log.debug("Kafka published | msgId={} partition={} offset={}",
                                event.messageId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
