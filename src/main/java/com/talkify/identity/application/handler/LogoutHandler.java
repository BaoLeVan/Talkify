package com.talkify.identity.application.handler;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.talkify.identity.application.command.LogoutCommand;
import com.talkify.identity.application.port.SessionCachePort;
import com.talkify.identity.domain.model.UserId;
import com.talkify.identity.domain.repository.SessionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogoutHandler {

    private final SessionRepository sessionRepository;
    private final SessionCachePort sessionCachePort;

    @Transactional
    public void handle(LogoutCommand command, UserId userId) {
        switch (command.scope()) {
            case CURRENT_SESSION_ONLY -> {
                sessionRepository.revokeById(command.sessionId());
                sessionCachePort.evictSession(command.sessionId(), userId);
                log.info("Session revoked (current) | userId={}", userId.value());
            }
            case ALL_EXCEPT_CURRENT -> {
                sessionRepository.revokeAllByUserIdExceptSessionId(userId, command.sessionId());
                var currentOpt = sessionRepository.findById(command.sessionId());
                sessionCachePort.evictAllSessions(userId);
                currentOpt.ifPresent(current ->
                    sessionCachePort.cacheSession(current.getId(), userId, current.getExpiresAt()));
                log.info("All other sessions revoked | userId={}", userId.value());
            }
            case ALL_SESSIONS -> {
                sessionRepository.revokeAllByUserId(userId);
                sessionCachePort.evictAllSessions(userId);
                log.info("All sessions revoked | userId={}", userId.value());
            }
        }
    }
}
