package com.talkify.messaging.infrastructure.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;
import com.talkify.messaging.application.port.TypingPort;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@Component
@RequiredArgsConstructor
public class TypingRedisAdapter implements TypingPort {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String TYPING_CHANNEL = "channel:conv:%s:typing";

    @Override
    @SneakyThrows
    public void publishTypingEvent(Long conversationId, Long userId, boolean isTyping) {
        String channel = String.format(TYPING_CHANNEL, conversationId);
        String payload = objectMapper.writeValueAsString(
            new TypingMessage(conversationId, userId, isTyping)
        );
        redisTemplate.convertAndSend(channel, payload);
    }

    public record TypingMessage(Long conversationId, Long senderId, boolean isTyping) {}
}
