package com.talkify.identity.application.handler;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.talkify.identity.application.port.SessionCachePort;
import com.talkify.identity.domain.event.SessionCreatedEvent;
import com.talkify.identity.domain.event.SessionRevokedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionEventListener {
    private final SessionCachePort sessionCachePort;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSessionCreated(SessionCreatedEvent event) {
        sessionCachePort.cacheSession(event.sessionId(), event.userId(), event.expiresAt());
        log.info("Session cached | sid={} uid={}", event.sessionId().value(), event.userId().value());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSessionRevoked(SessionRevokedEvent event) {
        sessionCachePort.evictSession(event.sessionId(), event.userId());
        log.info("Session evicted | sid={} uid={}", event.sessionId().value(), event.userId().value());
    }
}
