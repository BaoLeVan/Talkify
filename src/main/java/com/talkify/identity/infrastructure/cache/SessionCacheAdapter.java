package com.talkify.identity.infrastructure.cache;

import java.time.Duration;

import org.springframework.stereotype.Component;

import com.talkify.identity.application.port.CachePort;
import com.talkify.identity.application.port.SessionCachePort;
import com.talkify.identity.domain.model.SessionId;
import com.talkify.identity.domain.model.UserId;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionCacheAdapter implements SessionCachePort {
    private final CachePort cachePort;

    private String hashKey(UserId userId) {
        return "user:" + userId.value() + ":sessions";
    }

    @Override
    public void cacheSession(SessionId sessionId, UserId userId, Duration ttl) {
        String key = hashKey(userId);
        String field = String.valueOf(sessionId.value());
        String value = String.valueOf(System.currentTimeMillis() + ttl.toMillis());

        cachePort.hset(key, field, value, ttl);
        cachePort.expireIfGreater(key, ttl);
    }

    @Override
    public boolean isSessionValid(SessionId sessionId) {
        return cachePort.hget("session:" + sessionId.value(), "userId").isPresent();
    }

    @Override
    public void evictSession(SessionId sessionId) {
        cachePort.hdel("session:" + sessionId.value(), "userId");
    }

    @Override
    public void evictAllSessions(UserId userId) {
        cachePort.hdel(hashKey(userId));
    }
    
}
