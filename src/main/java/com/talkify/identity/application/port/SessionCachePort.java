package com.talkify.identity.application.port;

import java.time.Duration;

import com.talkify.identity.domain.model.SessionId;
import com.talkify.identity.domain.model.UserId;

public interface SessionCachePort {
    void cacheSession(SessionId sessionId, UserId userId, Duration ttl);
    boolean isSessionValid(SessionId sessionId);
    void evictSession(SessionId sessionId);
    void evictAllSessions(UserId userId);
}
