package com.talkify.config.cache;

import org.springframework.context.annotation.Configuration;

/**
 * Redis connection factory và StringRedisTemplate đã được Spring Boot
 * auto-configure từ spring.data.redis.* properties (host/port/password).
 * Không cần định nghĩa bean thủ công ở đây.
 */
@Configuration
public class RedisConfig {
}
