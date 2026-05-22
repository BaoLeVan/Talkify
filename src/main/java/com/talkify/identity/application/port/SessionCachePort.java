package com.talkify.identity.application.port;

import java.time.Instant;

import com.talkify.common.domain.UserId;
import com.talkify.identity.domain.model.SessionId;

public interface SessionCachePort {
    void cacheSession(SessionId sessionId, UserId userId, Instant expiresAt);
    boolean exists(SessionId sessionId, UserId userId);
    boolean isSessionValid(SessionId sessionId, UserId userId);
    void evictSession(SessionId sessionId, UserId userId);
    void evictAllSessions(UserId userId);
}
