package com.talkify.identity.application.port;

import java.time.Duration;
import java.util.Optional;

public interface CachePort {
    void set(String key, String value, Duration ttl);
    Optional<String> get(String key);
    void delete(String key);
    boolean exists(String key);
}
