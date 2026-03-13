package com.talkify.identity.application.port;

import java.time.Duration;
import java.util.Optional;

public interface CachePort {
    void set(String key, String value, Duration ttl);
    Optional<String> get(String key);
    Long getExpire(String key);
    void delete(String key);
    boolean exists(String key);

    /**
     * Atomic GET-then-DELETE (Redis GETDEL).
     * Dùng cho OTP verify để tránh race condition TOCTOU.
     */
    Optional<String> getAndDelete(String key);

    /**
     * Atomic increment counter, đặt TTL nếu key chưa tồn tại.
     * Dùng cho rate limiting / brute-force protection.
     * @return giá trị sau khi increment
     */
    long increment(String key, Duration ttl);
}
