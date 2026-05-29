package com.talkify.messaging.infrastructure.cache;

import java.util.Map;
import java.util.stream.Collectors;

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
    private static final String READS_KEY = "conv:%s:reads";

    @Override
    public Map<Long, Long> getReadReceiptsForConversation(Long conversationId) {
        String key = String.format(READS_KEY, conversationId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        return entries.entrySet().stream()
            .collect(Collectors.toMap(
                e -> Long.parseLong(e.getKey().toString()),
                e -> Long.parseLong(e.getValue().toString())
            ));
    }
}

