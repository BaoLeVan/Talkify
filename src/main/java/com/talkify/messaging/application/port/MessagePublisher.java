package com.talkify.messaging.application.port;

import com.talkify.messaging.domain.model.Message;

/**
 * Application-level outbound port for real-time message delivery.
 * 
 * WHY application layer (not domain):
 * - Real-time delivery (WebSocket push, Kafka fanout) is a USE CASE concern,
 *   not a core domain invariant. The domain doesn't care HOW the message 
 *   reaches recipients — it only cares that it's persisted correctly.
 * 
 * Adapters:
 * - NoOpMessagePublisher: current phase (no real-time yet)
 * - KafkaMessagePublisher: publish to "chat.messages" topic
 * - WebSocketMessagePublisher: push via STOMP/SockJS
 * - CompositeMessagePublisher: chain multiple publishers (Kafka → WebSocket fallback)
 * 
 * Integration pattern: Called AFTER successful persistence in the use case.
 * Failures here should NOT rollback the DB transaction (at-least-once delivery).
 */
public interface MessagePublisher {

    /**
     * Publish a message for real-time delivery to conversation participants.
     * Implementation can be async/fire-and-forget.
     */
    void publish(Message message);
}
