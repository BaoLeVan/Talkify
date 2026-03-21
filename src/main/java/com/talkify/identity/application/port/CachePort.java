package com.talkify.identity.application.port;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

public interface CachePort {
    void set(String key, String value, Duration ttl);
    Optional<String> get(String key);
    Long getExpire(String key);
    void delete(String key);
    boolean exists(String key);
    Optional<String> getAndDelete(String key);
    long increment(String key, Duration ttl);
    void hset(String hashKey, String field, String value, Duration ttl);
    Optional<String> hget(String hashKey, String field);
    Map<Object, Object> hget(String hashKey);
    void hdel(String hashKey, String field);
    void hdel(String hashKey);
    void expireIfGreater(String key, Duration ttl);
    boolean setIfAbsent(String key, String value, Duration ttl);
}
