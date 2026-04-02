package com.talkify.identity.application.handler;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.talkify.common.exception.AppException;
import com.talkify.common.exception.ErrorCode;
import com.talkify.common.util.Sha256Utils;
import com.talkify.identity.application.command.RefreshTokenCommand;
import com.talkify.identity.application.dto.SessionResult;
import com.talkify.identity.application.dto.response.AuthResponse;
import com.talkify.identity.application.port.CachePort;
import com.talkify.identity.application.port.JwtPort;
import com.talkify.identity.application.port.SessionCachePort;
import com.talkify.identity.application.service.SessionService;
import com.talkify.identity.domain.event.SessionRevokedEvent;
import com.talkify.identity.domain.event.SessionRotationCompletedEvent;
import com.talkify.identity.domain.model.DeviceInfo;
import com.talkify.identity.domain.model.User;
import com.talkify.identity.domain.model.UserSession;
import com.talkify.identity.domain.repository.SessionRepository;
import com.talkify.identity.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionHandler {

    private static final Duration ROTATION_LOCK_TTL = Duration.ofSeconds(5);

    private final ApplicationEventPublisher    eventPublisher;
    private final CachePort         cachePort;
    private final JwtPort           jwtPort;
    private final SessionService    sessionService;
    private final SessionRepository sessionRepository;
    private final UserRepository    userRepository;
    private final Clock             clock;

    @Transactional
    public AuthResponse handle(RefreshTokenCommand command, DeviceInfo deviceInfo) {
        String rawToken  = command.refreshToken();
        String tokenHash = Sha256Utils.hash(rawToken);

        if (!jwtPort.validateRefreshToken(rawToken)) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        String lockKey = "lock:refresh:" + tokenHash;
        if (!cachePort.setIfAbsent(lockKey, "1", ROTATION_LOCK_TTL)) {
            log.warn("Refresh lock contention | tokenHash prefix={}...", tokenHash.substring(0, 8));
            throw new AppException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }

        eventPublisher.publishEvent(new SessionRotationCompletedEvent(lockKey));
        return doHandle(tokenHash, rawToken, deviceInfo);
    }

    private AuthResponse doHandle(String tokenHash, String rawToken, DeviceInfo deviceInfo) {
        UserSession session = sessionRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        if (session.isRevoked()) {
            log.warn("Refresh token reuse detected | userId={}", session.getUserId().value());
            sessionRepository.revokeAllByUserId(session.getUserId());
            throw new AppException(ErrorCode.REFRESH_TOKEN_REUSE_DETECTED);
        }

        if (session.isExpired(Instant.now())) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        User user = userRepository.findById(session.getUserId().value())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.isBanned())   throw new AppException(ErrorCode.USER_BANNED);
        if (user.isDeleted())  throw new AppException(ErrorCode.INVALID_CREDENTIALS);

        long remainingSeconds = Duration.between(clock.instant(), session.getExpiresAt()).getSeconds();

        if (remainingSeconds > jwtPort.refreshThreshold()) {
            return handleReactiveRenewal(session, user, remainingSeconds, rawToken);
        } else {
            return handleProactiveRotation(session, user, tokenHash, deviceInfo);
        }
    }

    private AuthResponse handleReactiveRenewal(UserSession session, User user,
                                               long remainingSeconds, String rawToken) {
        session.markUsed(Instant.now());
        sessionRepository.save(session);

        String newAccessToken = jwtPort.issueAccessToken(
                user.getId(), session.getId(), user.getRole(), user.getStatus());

        log.debug("Access token renewed | userId={} remainingTtl={}s",
                user.getId().value(), remainingSeconds);
        return AuthResponse.of(newAccessToken, rawToken, null);
    }

    private AuthResponse handleProactiveRotation(UserSession session, User user,
                                                 String tokenHash, DeviceInfo deviceInfo) {
        sessionRepository.revokeByTokenHash(tokenHash);
        eventPublisher.publishEvent(new SessionRevokedEvent(session.getId(), user.getId()));

        SessionResult sessionResult = sessionService.createSession(user.getId(), deviceInfo);
        String newAccessToken = jwtPort.issueAccessToken(
                user.getId(), sessionResult.sessionId(), user.getRole(), user.getStatus());

        log.info("Refresh token rotated | userId={}", user.getId().value());
        return AuthResponse.of(newAccessToken, sessionResult.rawRefreshToken(), null);
    }
}

