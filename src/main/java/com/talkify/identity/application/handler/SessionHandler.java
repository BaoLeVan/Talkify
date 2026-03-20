package com.talkify.identity.application.handler;

import java.time.Clock;
import java.time.Duration;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.talkify.common.exception.AppException;
import com.talkify.common.exception.ErrorCode;
import com.talkify.common.util.Sha256Utils;
import com.talkify.identity.application.command.RefreshTokenCommand;
import com.talkify.identity.application.dto.SessionResult;
import com.talkify.identity.application.dto.response.AuthResponse;
import com.talkify.identity.application.port.JwtPort;
import com.talkify.identity.application.port.TokenClaims;
import com.talkify.identity.application.service.SessionService;
import com.talkify.identity.domain.model.DeviceInfo;
import com.talkify.identity.domain.model.User;
import com.talkify.identity.domain.model.UserSession;
import com.talkify.identity.domain.repository.SessionRepository;
import com.talkify.identity.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Xử lý refresh token theo chiến lược Hybrid:
 *
 *  ┌─────────────────────────────────────────────────────────┐
 *  │  remaining TTL > threshold (JWT_REFRESH_THRESHOLD)       │
 *  │   → chỉ cấp access token mới, GIỮ NGUYÊN refresh token  │
 *  ├─────────────────────────────────────────────────────────┤
 *  │  remaining TTL ≤ threshold (gần hết hạn)                 │
 *  │   → rotation: revoke old session, cấp cặp token mới      │
 *  └─────────────────────────────────────────────────────────┘
 *
 *  Reuse attack detection:
 *   Nếu refresh token hợp lệ về chữ ký (JWT) nhưng đã bị revoke trong DB
 *   → ai đó đang dùng token cũ đã bị thu hồi → revoke ALL sessions ngay lập tức.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionHandler {

    private final JwtPort           jwtPort;
    private final SessionService    sessionService;
    private final SessionRepository sessionRepository;
    private final UserRepository    userRepository;
    private final Clock             clock;

    @Transactional
    public AuthResponse handle(RefreshTokenCommand command, DeviceInfo deviceInfo) {
        String rawToken = command.refreshToken();

        if (!jwtPort.validateToken(rawToken)) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        TokenClaims claims = jwtPort.extractAllClaims(rawToken);
        if (!"refresh".equals(claims.type())) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        String tokenHash = Sha256Utils.hash(rawToken);
        UserSession session = sessionRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        if (session.isRevoked()) {
            log.warn("Refresh token reuse detected | userId={}", session.getUserId().value());
            sessionRepository.revokeAllByUserId(session.getUserId());
            throw new AppException(ErrorCode.REFRESH_TOKEN_REUSE_DETECTED);
        }

        if (session.isExpired()) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        User user = userRepository.findById(session.getUserId().value())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.isBanned())   throw new AppException(ErrorCode.USER_BANNED);
        if (user.isDeleted())  throw new AppException(ErrorCode.INVALID_CREDENTIALS);

        long remainingSeconds = Duration.between(clock.instant(), session.getExpiresAt()).getSeconds();

        if (remainingSeconds > jwtPort.getRefreshThreshold()) {
            // Reactive renewal: giữ RT cũ, chỉ cấp AT mới với sessionId hiện tại
            session.markUsed();
            sessionRepository.save(session);

            String newAccessToken = jwtPort.generateAccessToken(
                    user.getId(), session.getId(), user.getRole(), user.getStatus());

            log.debug("Access token renewed | userId={} remainingTtl={}s",
                    user.getId().value(), remainingSeconds);
            return AuthResponse.of(newAccessToken, rawToken, null);

        } else {
            // Proactive rotation: revoke session cũ, tạo session mới
            sessionRepository.revokeByTokenHash(tokenHash);

            SessionResult sessionResult = sessionService.createSession(user.getId(), deviceInfo);
            String newAccessToken = jwtPort.generateAccessToken(
                    user.getId(), sessionResult.sessionId(), user.getRole(), user.getStatus());

            log.info("Refresh token rotated | userId={} remainingTtl={}s",
                    user.getId().value(), remainingSeconds);
            return AuthResponse.of(newAccessToken, sessionResult.rawRefreshToken(), null);
        }
    }
}

