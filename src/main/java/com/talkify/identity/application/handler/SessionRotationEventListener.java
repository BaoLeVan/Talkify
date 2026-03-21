package com.talkify.identity.application.handler;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.talkify.identity.application.port.CachePort;
import com.talkify.identity.domain.event.SessionRotationCompletedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionRotationEventListener {

    private final CachePort cachePort;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRotationCommitted(SessionRotationCompletedEvent event) {
        cachePort.delete(event.lockKey());
        log.debug("Rotation lock released [AFTER_COMMIT] | key={}", event.lockKey());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void onRotationRolledBack(SessionRotationCompletedEvent event) {
        cachePort.delete(event.lockKey());
        log.warn("Rotation lock released [AFTER_ROLLBACK] | key={}", event.lockKey());
    }
}
