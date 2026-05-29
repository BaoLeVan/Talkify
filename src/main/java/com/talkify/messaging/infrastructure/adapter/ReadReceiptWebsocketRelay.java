package com.talkify.messaging.infrastructure.adapter;

import java.util.Map;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.talkify.messaging.infrastructure.cache.ReadReceiptRedisAdapter.ReadReceiptMessage;

import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReadReceiptWebsocketRelay implements MessageListener {
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            var msg = objectMapper.readValue(new String(message.getBody()), ReadReceiptMessage.class);
            log.info("Received read receipt message: conversationId={}, readerId={}, sequenceNumber={}, readAt={}",
                msg.conversationId(), msg.readerid(), msg.sequenceNumber(), msg.readAt());

            var payload = Map.of(
                "readerId", msg.readerid(),
                "sequenceNumber", msg.sequenceNumber(),
                "readAt", msg.readAt()
            );

            simpMessagingTemplate.convertAndSend(
                "/topic/conversations/%s/read-receipts".formatted(msg.conversationId()),
                payload
            );
        } catch (Exception e) {
            log.error("Failed to process read receipt message: " + e.getMessage(), e);
            return;
        }
    }
}
