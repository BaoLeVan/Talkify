package com.talkify.messaging.infrastructure.websocket.adapter;

import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class TypingWebsocketRelay implements MessageListener {
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte @Nullable [] pattern) {
        var msg = objectMapper.readValue(new String(message.getBody()), TypingMessage.class);
        log.info("Received typing message: senderId={}, isTyping={}",
            msg.senderId(), msg.isTyping());

        Object payload = Map.of(
            "senderId", msg.senderId(),
            "isTyping", msg.isTyping()
        );

        simpMessagingTemplate.convertAndSend(
            "/topic/conversations/%s/typing".formatted(extractConversationId(message)),
            payload
        );
    }

    private Long extractConversationId(Message message) {
        String channel = new String(message.getChannel());
        String[] parts = channel.split(":");
        return Long.parseLong(parts[2]);
    }

    private record TypingMessage(
        Long senderId,
        boolean isTyping
    ) {}
    
}
