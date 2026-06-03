package com.talkify.messaging.infrastructure.websocket.adapter;

import java.util.Map;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.talkify.messaging.infrastructure.cache.ReadReceiptRedisAdapter.ReadReceiptMessage;

import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Driven adapter: nhận read receipt event từ Redis Pub/Sub → push xuống WebSocket client.
 *
 * Vị trí đúng trong kiến trúc:
 *   Redis Pub/Sub (external)  →  [ReadReceiptWebsocketRelay]  →  WebSocket client
 *
 * Tại sao KHÔNG cần port interface:
 *   Implements MessageListener (Spring Data Redis) — Spring gọi onMessage() tự động
 *   khi Redis publish message matching pattern đã subscribe.
 *   Không có application layer involvement.
 *
 * Chuẩn bị microservice:
 *   Khi tách WS service, class này chuyển sang dedicated WS gateway.
 *   Redis Pub/Sub vẫn là integration channel — không cần thay đổi publisher side.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReadReceiptWebsocketRelay implements MessageListener {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            var msg = objectMapper.readValue(new String(message.getBody()), ReadReceiptMessage.class);
            log.debug("Relaying read receipt: conversationId={}, readerId={}, seq={}",
                msg.conversationId(), msg.readerid(), msg.sequenceNumber());

            Object payload = Map.of(
                "readerId",       msg.readerid(),
                "sequenceNumber", msg.sequenceNumber(),
                "readAt",         msg.readAt()
            );
            simpMessagingTemplate.convertAndSend(
                "/topic/conversations/%s/read-receipts".formatted(msg.conversationId()),
                payload
            );
        } catch (Exception e) {
            log.error("Failed to relay read receipt: {}", e.getMessage(), e);
        }
    }
}
