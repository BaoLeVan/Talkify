package com.talkify.messaging.infrastructure.event;

import org.springframework.stereotype.Component;

import com.talkify.messaging.application.port.MessagePublisher;
import com.talkify.messaging.domain.model.Message;

import lombok.extern.slf4j.Slf4j;

/**
 * No-op implementation of MessagePublisher.
 * 
 * Current phase: real-time delivery is not yet implemented.
 * This placeholder satisfies the dependency injection contract without
 * adding any behavior.
 * 
 * NEXT STEPS (when integrating Kafka/WebSocket):
 * 1. Create KafkaMessagePublisher implements MessagePublisher
 *    - Serialize message to Avro/Protobuf
 *    - Publish to "chat.messages.{conversationId}" topic
 * 2. Create WebSocketMessagePublisher implements MessagePublisher
 *    - Push via STOMP to /topic/conversations/{id}
 * 3. Create CompositeMessagePublisher that chains both
 * 4. Remove this NoOp and update @Primary or @ConditionalOnProperty
 * 
 * Configuration example:
 *   @ConditionalOnProperty(name = "talkify.messaging.publisher", havingValue = "kafka")
 *   public class KafkaMessagePublisher implements MessagePublisher { ... }
 */
@Slf4j
@Component
public class NoOpMessagePublisher implements MessagePublisher {

    @Override
    public void publish(Message message) {
        log.debug("NoOp: Message {} would be published for real-time delivery " +
                  "(Kafka/WebSocket not yet configured)", message.getId().value());
    }
}
