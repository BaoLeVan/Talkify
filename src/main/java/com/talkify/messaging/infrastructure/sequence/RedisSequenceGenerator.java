package com.talkify.messaging.infrastructure.sequence;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.talkify.messaging.domain.port.SequenceGenerator;

import lombok.RequiredArgsConstructor;

/**
 * Redis-based implementation of SequenceGenerator.
 * 
 * Uses Redis INCR for atomic, distributed-safe sequence allocation.
 * 
 * WHY Redis:
 * - INCR is atomic and O(1) — no locks needed
 * - Distributed-safe: works across multiple app instances
 * - Extremely low latency (~0.1ms per call)
 * 
 * TRADE-OFFS:
 * - If Redis loses data (non-persistent), sequences may reset → duplicate sequences
 *   Mitigation: Use Redis AOF persistence or initialize from DB max(sequenceNumber)
 * - Gap-free is NOT guaranteed (if app crashes between INCR and MongoDB write)
 *   This is acceptable for chat — gaps don't affect correctness, only aesthetics
 * 
 * Key format: "conv:seq:{conversationId}" → monotonically increasing counter
 */
@Component
@RequiredArgsConstructor
public class RedisSequenceGenerator implements SequenceGenerator {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "conv:seq:";

    @Override
    public long nextSequence(long conversationId) {
        String key = KEY_PREFIX + conversationId;
        Long value = redisTemplate.opsForValue().increment(key);
        // Redis INCR returns the new value after increment, starting from 1
        return value != null ? value : 1L;
    }
}
