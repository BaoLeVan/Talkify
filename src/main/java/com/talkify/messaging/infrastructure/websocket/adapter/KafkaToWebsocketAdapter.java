package com.talkify.messaging.infrastructure.websocket.adapter;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.talkify.messaging.application.event.MessageDispatchEvent;
import com.talkify.messaging.infrastructure.kafka.KafkaConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Driven adapter: nhận MessageDispatchEvent từ Kafka → push xuống WebSocket client.
 *
 * Vị trí đúng trong kiến trúc:
 *   Kafka (external)  →  [KafkaToWebsocketAdapter]  →  WebSocket client
 *
 * Tại sao KHÔNG cần application layer / port interface:
 *   Adapter này là pure pipeline: consume Kafka → deliver via STOMP.
 *   Không có business logic, không có state, không có decision-making.
 *   Application layer không gọi adapter này — Kafka listener tự động trigger.
 *
 * Chuẩn bị microservice:
 *   Khi tách WS thành dedicated gateway service, class này chuyển sang service đó.
 *   Kafka topic vẫn là integration point — không cần thay đổi producer side.
 *   SimpMessagingTemplate được thay bằng direct STOMP session management.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaToWebsocketAdapter {

    private final SimpMessagingTemplate messageTemplate;

    @KafkaListener(topics = KafkaConfig.CHAT_TOPIC, groupId = "${spring.kafka.consumer.group-id}")
    public void consume(MessageDispatchEvent event) {
        if (event.recipientIds() == null || event.recipientIds().isEmpty()) {
            log.warn("No recipients | msgId={}", event.messageId());
            return;
        }

        switch (event.conversationType()) {
            case "GROUP"  -> dispatchGroup(event);
            case "DIRECT" -> dispatchDirect(event);
            default -> log.warn("Unknown conversationType={} | msgId={}",
                    event.conversationType(), event.messageId());
        }
    }

    private void dispatchGroup(MessageDispatchEvent event) {
        String destination = "/topic/conversations/" + event.conversationId();
        messageTemplate.convertAndSend(destination, event);
        log.debug("WS GROUP | dest={} msgId={}", destination, event.messageId());
    }

    private void dispatchDirect(MessageDispatchEvent event) {
        for (Long userId : event.recipientIds()) {
            messageTemplate.convertAndSendToUser(userId.toString(), "/queue/messages", event);
            log.debug("WS DIRECT | userId={} msgId={}", userId, event.messageId());
        }
    }
}
