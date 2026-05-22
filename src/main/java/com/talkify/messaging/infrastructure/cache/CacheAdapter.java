package com.talkify.messaging.infrastructure.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.talkify.messaging.application.port.CachePort;

import lombok.RequiredArgsConstructor;

/**
 * @deprecated This adapter is kept for backward compatibility.
 * Sequence generation is now handled by {@code RedisSequenceGenerator}.
 * Other caching concerns can be added here as needed.
 */
@Deprecated(forRemoval = true)
@Component("messagingCacheAdapter")
@RequiredArgsConstructor
public class CacheAdapter implements CachePort {
    private final StringRedisTemplate redisTemplate;

    @Override
    public long getNextSequenceNumber(Long conversationId) {
        Long value = redisTemplate.opsForValue().increment("conv:seq:" + conversationId.toString());
        return value != null ? value : 1L;
    }
}

