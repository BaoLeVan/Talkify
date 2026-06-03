package com.talkify.config.cache;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import com.talkify.messaging.infrastructure.websocket.adapter.ReadReceiptWebsocketRelay;
import com.talkify.messaging.infrastructure.websocket.adapter.TypingWebsocketRelay;

import lombok.RequiredArgsConstructor;

/**
 * Redis connection factory và StringRedisTemplate đã được Spring Boot
 * auto-configure từ spring.data.redis.* properties (host/port/password).
 * Không cần định nghĩa bean thủ công ở đây.
 */
@Configuration
@RequiredArgsConstructor
public class RedisConfig {
    private final ReadReceiptWebsocketRelay readReceiptWebsocketRelay;
    private final TypingWebsocketRelay typingWebsocketRelay;

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            org.springframework.data.redis.connection.RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(readReceiptWebsocketRelay, new PatternTopic("channel:conv:*:read-receipts"));
        container.addMessageListener(typingWebsocketRelay, new PatternTopic("channel:conv:*:typing"));
        return container;
    }
}
