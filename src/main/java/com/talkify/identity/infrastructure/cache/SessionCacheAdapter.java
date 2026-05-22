package com.talkify.identity.infrastructure.cache;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Component;

import com.talkify.common.domain.UserId;
import com.talkify.identity.application.port.CachePort;
import com.talkify.identity.application.port.SessionCachePort;
import com.talkify.identity.domain.model.SessionId;

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
    public void cacheSession(SessionId sessionId, UserId userId, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            log.warn("SessionCache | skip already-expired session sid={}", sessionId.value());
            return;
        }
        String key   = hashKey(userId);
        String field = String.valueOf(sessionId.value());
        String value = String.valueOf(expiresAt.getEpochSecond());

        cachePort.hset(key, field, value, ttl);
        cachePort.expireIfGreater(key, ttl);
        log.debug("SessionCache | cached sid={} uid={} ttl={}s", sessionId.value(), userId.value(), ttl.toSeconds());
    }

    @Override
    public boolean isSessionValid(SessionId sessionId, UserId userId) {
        return cachePort.hget(hashKey(userId), String.valueOf(sessionId.value()))
                .map(epochSecond -> Instant.now().isBefore(Instant.ofEpochSecond(Long.parseLong(epochSecond))))
                .orElse(false);
    }

    @Override
    public void evictSession(SessionId sessionId, UserId userId) {
        cachePort.hdel(hashKey(userId), String.valueOf(sessionId.value()));
        log.debug("SessionCache | evicted sid={} uid={}", sessionId.value(), userId.value());
    }

    @Override
    public void evictAllSessions(UserId userId) {
        cachePort.hdel(hashKey(userId));
        log.debug("SessionCache | evicted ALL sessions uid={}", userId.value());
    }

    @Override
    public boolean exists(SessionId sessionId, UserId userId) {
        return cachePort.hget(hashKey(userId), String.valueOf(sessionId.value())).isPresent();
    }
}
