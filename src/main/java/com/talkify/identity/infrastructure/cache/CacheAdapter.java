package com.talkify.identity.infrastructure.cache;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.talkify.identity.application.port.CachePort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheAdapter implements CachePort {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void set(String key, String value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
        log.debug("Cache SET | key={} ttl={}s", key, ttl.toSeconds());
    }

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key));
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Atomic GETDEL — eliminates TOCTOU race condition in OTP verify.
     * Redis >= 6.2 supports GETDEL natively.
     */
    @Override
    public Optional<String> getAndDelete(String key) {
        String value = redisTemplate.opsForValue().getAndDelete(key);
        log.debug("Cache GETDEL | key={} found={}", key, value != null);
        return Optional.ofNullable(value);
    }

    /**
     * Atomic INCR + SET TTL on first creation.
     * Used for brute-force attempt counting.
     */
    @Override
    public long increment(String key, Duration ttl) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, ttl.toSeconds(), TimeUnit.SECONDS);
        }
        long result = count != null ? count : 1L;
        log.debug("Cache INCR | key={} count={}", key, result);
        return result;
    }

    @Override
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key);
    }

    @Override
    public void hset(String hashKey, String field, String value, Duration ttl) {
        redisTemplate.opsForHash().put(hashKey, field, value);
        redisTemplate.expire(hashKey, ttl.toSeconds(), TimeUnit.SECONDS);
        log.debug("Cache HSET | hashKey={} field={} ttl={}s", hashKey, field, ttl.toSeconds());
    }

    @Override
    public Optional<String> hget(String hashKey, String field) {
        Object value = redisTemplate.opsForHash().get(hashKey, field);
        return Optional.ofNullable(value != null ? value.toString() : null);
    }

    @Override
    public void hdel(String hashKey, String field) {
        redisTemplate.opsForHash().delete(hashKey, field);
        log.debug("Cache HDEL | hashKey={} field={}", hashKey, field);
    }

    @Override
    public void hdel(String hashKey) {
        redisTemplate.delete(hashKey);
        log.debug("Cache HDEL | hashKey={}", hashKey);
    }

    @Override
    public void expireIfGreater(String key, Duration ttl) {
        Long currentTtl = redisTemplate.getExpire(key);
        if (currentTtl == null || currentTtl < ttl.toSeconds()) {
            redisTemplate.expire(key, ttl.toSeconds(), TimeUnit.SECONDS);
            log.debug("Cache EXPIRE | key={} newTtl={}s", key, ttl.toSeconds());
        } else {
            log.debug("Cache EXPIRE SKIP | key={} currentTtl={}s >= newTtl={}s", key, currentTtl, ttl.toSeconds());
        }
    }
}
