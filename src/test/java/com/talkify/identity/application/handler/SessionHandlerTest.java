package com.talkify.identity.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import com.talkify.identity.domain.model.DevicePlatform;
import com.talkify.identity.domain.model.Email;
import com.talkify.identity.domain.model.Password;
import com.talkify.identity.domain.model.SessionId;
import com.talkify.identity.domain.model.User;
import com.talkify.identity.domain.model.UserId;
import com.talkify.identity.domain.model.UserRole;
import com.talkify.identity.domain.model.UserSession;
import com.talkify.identity.domain.model.UserStatus;
import com.talkify.identity.domain.repository.SessionRepository;
import com.talkify.identity.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionHandler — Refresh Token")
class SessionHandlerTest {

    @Mock private JwtPort jwtPort;
    @Mock private SessionService sessionService;
    @Mock private SessionRepository sessionRepository;
    @Mock private UserRepository userRepository;
    @Mock private Clock clock;

    @InjectMocks private SessionHandler handler;

    // ── Constants ────────────────────────────────────────────────────────────
    private static final Instant FIXED_NOW     = Instant.now();
    private static final UserId USER_ID        = UserId.of(1L);
    private static final String RAW_TOKEN      = "raw-refresh-jwt-token";
    private static final String TOKEN_HASH     = Sha256Utils.hash(RAW_TOKEN);
    private static final String NEW_ACCESS     = "new-access-token";
    private static final String NEW_REFRESH    = "new-refresh-token";
    private static final long   THRESHOLD      = 86400L;  // 1 day
    private static final String HASHED_PW      = "$2a$10$hashed";
    private static final DeviceInfo DEVICE_INFO  = DeviceInfo.of("Chrome on macOS", DevicePlatform.WEB, "203.0.113.1");

    private static final TokenClaims REFRESH_CLAIMS = new TokenClaims(
            "1", "refresh", null, null, null);
    private static final TokenClaims ACCESS_CLAIMS = new TokenClaims(
            "1", "access", "USER", "ACTIVE", null);

    // ── Helpers ──────────────────────────────────────────────────────────────

    private RefreshTokenCommand command() {
        return new RefreshTokenCommand(RAW_TOKEN);
    }

    @BeforeEach
    void setupClock() {
        // Provide a deterministic "now" so remainingSeconds calculations are stable.
        // lenient() because early-fail tests (invalid JWT, revoked session, etc.) throw
        // before reaching the clock.instant() call — strict stubbing would flag them.
        lenient().when(clock.instant()).thenReturn(FIXED_NOW);
    }

    private User buildUser(UserStatus status) {
        return User.reconstruct(
                USER_ID, "testuser", "Test User",
                Email.of("test@gmail.com"), "+84912345678",
                Password.ofHashed(HASHED_PW),
                UserRole.USER, status,
                null, Instant.now(), null
        );
    }

    /** Session còn thoải mái TTL (> threshold) */
    private UserSession activeSession() {
        return UserSession.reconstruct(
                SessionId.of(100L), USER_ID, TOKEN_HASH,
                DeviceInfo.of("Chrome", DevicePlatform.WEB, "127.0.0.1"),
                FIXED_NOW.plusSeconds(THRESHOLD + 3600), // còn > threshold
                FIXED_NOW, FIXED_NOW, null
        );
    }

    /** Session sắp hết hạn (remaining ≤ threshold) */
    private UserSession nearExpirySession() {
        return UserSession.reconstruct(
                SessionId.of(101L), USER_ID, TOKEN_HASH,
                DeviceInfo.of("Chrome", DevicePlatform.WEB, "127.0.0.1"),
                FIXED_NOW.plusSeconds(THRESHOLD - 100), // còn < threshold
                FIXED_NOW, FIXED_NOW, null
        );
    }

    /** Session đã bị revoke */
    private UserSession revokedSession() {
        return UserSession.reconstruct(
                SessionId.of(102L), USER_ID, TOKEN_HASH,
                DeviceInfo.of("Chrome", DevicePlatform.WEB, "127.0.0.1"),
                FIXED_NOW.plusSeconds(3600),
                FIXED_NOW, FIXED_NOW, FIXED_NOW // revokedAt != null
        );
    }

    /** Session đã expired */
    private UserSession expiredSession() {
        return UserSession.reconstruct(
                SessionId.of(103L), USER_ID, TOKEN_HASH,
                DeviceInfo.of("Chrome", DevicePlatform.WEB, "127.0.0.1"),
                FIXED_NOW.minusSeconds(100), // đã hết hạn
                FIXED_NOW, FIXED_NOW, null
        );
    }

    private void stubJwtValid() {
        when(jwtPort.validateToken(RAW_TOKEN)).thenReturn(true);
        when(jwtPort.extractAllClaims(RAW_TOKEN)).thenReturn(REFRESH_CLAIMS);
    }

    private void stubThreshold() {
        when(jwtPort.getRefreshThreshold()).thenReturn(THRESHOLD);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  1. JWT Validation Failures
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("JWT validation failures")
    class JwtValidation {

        @Test
        @DisplayName("should throw INVALID_TOKEN when JWT signature is invalid")
        void invalidSignature() {
            when(jwtPort.validateToken(RAW_TOKEN)).thenReturn(false);

            assertThatThrownBy(() -> handler.handle(command(), DEVICE_INFO))
                    .isInstanceOf(AppException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_TOKEN);

            verify(sessionRepository, never()).findByTokenHash(anyString());
        }

        @Test
        @DisplayName("should throw INVALID_TOKEN when token type is not 'refresh'")
        void wrongTokenType() {
            when(jwtPort.validateToken(RAW_TOKEN)).thenReturn(true);
            when(jwtPort.extractAllClaims(RAW_TOKEN)).thenReturn(ACCESS_CLAIMS);

            assertThatThrownBy(() -> handler.handle(command(), DEVICE_INFO))
                    .isInstanceOf(AppException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_TOKEN);

            verify(sessionRepository, never()).findByTokenHash(anyString());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  2. Session Lookup & State
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Session state checks")
    class SessionState {

        @Test
        @DisplayName("should throw SESSION_NOT_FOUND when no session exists for token hash")
        void sessionNotFound() {
            stubJwtValid();
            when(sessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> handler.handle(command(), DEVICE_INFO))
                    .isInstanceOf(AppException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.SESSION_NOT_FOUND);
        }

        @Test
        @DisplayName("should revoke ALL sessions and throw REUSE_DETECTED when session is already revoked")
        void reuseAttack() {
            stubJwtValid();
            when(sessionRepository.findByTokenHash(TOKEN_HASH))
                    .thenReturn(Optional.of(revokedSession()));

            assertThatThrownBy(() -> handler.handle(command(), DEVICE_INFO))
                    .isInstanceOf(AppException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.REFRESH_TOKEN_REUSE_DETECTED);

            verify(sessionRepository).revokeAllByUserId(USER_ID);
        }

        @Test
        @DisplayName("should throw REFRESH_TOKEN_EXPIRED when session is expired")
        void sessionExpired() {
            stubJwtValid();
            when(sessionRepository.findByTokenHash(TOKEN_HASH))
                    .thenReturn(Optional.of(expiredSession()));

            assertThatThrownBy(() -> handler.handle(command(), DEVICE_INFO))
                    .isInstanceOf(AppException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  3. User Status Checks
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("User status checks")
    class UserStatusChecks {

        @Test
        @DisplayName("should throw USER_NOT_FOUND when user does not exist")
        void userNotFound() {
            stubJwtValid();
            when(sessionRepository.findByTokenHash(TOKEN_HASH))
                    .thenReturn(Optional.of(activeSession()));
            when(userRepository.findById(USER_ID.value())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> handler.handle(command(), DEVICE_INFO))
                    .isInstanceOf(AppException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("should throw USER_BANNED when user is banned")
        void userBanned() {
            stubJwtValid();
            when(sessionRepository.findByTokenHash(TOKEN_HASH))
                    .thenReturn(Optional.of(activeSession()));
            when(userRepository.findById(USER_ID.value()))
                    .thenReturn(Optional.of(buildUser(UserStatus.BANNED)));

            assertThatThrownBy(() -> handler.handle(command(), DEVICE_INFO))
                    .isInstanceOf(AppException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.USER_BANNED);
        }

        @Test
        @DisplayName("should throw INVALID_CREDENTIALS when user is deleted")
        void userDeleted() {
            stubJwtValid();
            when(sessionRepository.findByTokenHash(TOKEN_HASH))
                    .thenReturn(Optional.of(activeSession()));
            when(userRepository.findById(USER_ID.value()))
                    .thenReturn(Optional.of(buildUser(UserStatus.DELETED)));

            assertThatThrownBy(() -> handler.handle(command(), DEVICE_INFO))
                    .isInstanceOf(AppException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  4. Hybrid Strategy — Reactive Renew (TTL > threshold)
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Reactive renew — TTL > threshold")
    class ReactiveRenew {

        @Test
        @DisplayName("should return new access token + SAME refresh token when TTL is large")
        void renewAccessOnly() {
            stubJwtValid();
            UserSession session = activeSession();
            when(sessionRepository.findByTokenHash(TOKEN_HASH))
                    .thenReturn(Optional.of(session));
            when(userRepository.findById(USER_ID.value()))
                    .thenReturn(Optional.of(buildUser(UserStatus.ACTIVE)));
            when(jwtPort.generateAccessToken(eq(USER_ID), any(SessionId.class), eq(UserRole.USER), eq(UserStatus.ACTIVE)))
                    .thenReturn(NEW_ACCESS);
            stubThreshold();
            when(sessionRepository.save(any(UserSession.class))).thenReturn(session);

            AuthResponse response = handler.handle(command(), DEVICE_INFO);

            assertThat(response.accessToken()).isEqualTo(NEW_ACCESS);
            assertThat(response.refreshToken()).isEqualTo(RAW_TOKEN); // unchanged
            verify(sessionRepository).save(session);
            verify(sessionRepository, never()).revokeByTokenHash(anyString());
            verify(jwtPort, never()).generateRefreshToken(any());
        }

        @Test
        @DisplayName("should work for INACTIVE user (not banned/deleted)")
        void inactiveUserOk() {
            stubJwtValid();
            UserSession session = activeSession();
            when(sessionRepository.findByTokenHash(TOKEN_HASH))
                    .thenReturn(Optional.of(session));
            when(userRepository.findById(USER_ID.value()))
                    .thenReturn(Optional.of(buildUser(UserStatus.INACTIVE)));
            when(jwtPort.generateAccessToken(eq(USER_ID), any(SessionId.class), eq(UserRole.USER), eq(UserStatus.INACTIVE)))
                    .thenReturn(NEW_ACCESS);
            stubThreshold();
            when(sessionRepository.save(any(UserSession.class))).thenReturn(session);

            AuthResponse response = handler.handle(command(), DEVICE_INFO);

            assertThat(response.accessToken()).isEqualTo(NEW_ACCESS);
            assertThat(response.refreshToken()).isEqualTo(RAW_TOKEN);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  5. Hybrid Strategy — Proactive Rotation (TTL ≤ threshold)
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Proactive rotation — TTL ≤ threshold")
    class ProactiveRotation {

        @Test
        @DisplayName("should revoke old session, create new session, return new token pair")
        void rotateTokens() {
            stubJwtValid();
            when(sessionRepository.findByTokenHash(TOKEN_HASH))
                    .thenReturn(Optional.of(nearExpirySession()));
            when(userRepository.findById(USER_ID.value()))
                    .thenReturn(Optional.of(buildUser(UserStatus.ACTIVE)));
            when(sessionService.createSession(any(), any())).thenReturn(new SessionResult(new SessionId(999L), NEW_REFRESH));
            when(jwtPort.generateAccessToken(eq(USER_ID), any(SessionId.class), eq(UserRole.USER), eq(UserStatus.ACTIVE)))
                    .thenReturn(NEW_ACCESS);
            stubThreshold();

            AuthResponse response = handler.handle(command(), DEVICE_INFO);

            assertThat(response.accessToken()).isEqualTo(NEW_ACCESS);
            assertThat(response.refreshToken()).isEqualTo(NEW_REFRESH);
            verify(sessionRepository).revokeByTokenHash(TOKEN_HASH);
            verify(sessionService).createSession(any(), any());
        }

        @Test
        @DisplayName("should pass DeviceInfo from request to sessionService when rotating")
        void usesDeviceInfoFromRequest() {
            stubJwtValid();
            when(sessionRepository.findByTokenHash(TOKEN_HASH))
                    .thenReturn(Optional.of(nearExpirySession()));
            when(userRepository.findById(USER_ID.value()))
                    .thenReturn(Optional.of(buildUser(UserStatus.ACTIVE)));
            when(sessionService.createSession(any(), eq(DEVICE_INFO))).thenReturn(new SessionResult(new SessionId(999L), NEW_REFRESH));
            when(jwtPort.generateAccessToken(any(), any(), any(), any())).thenReturn(NEW_ACCESS);
            stubThreshold();

            AuthResponse response = handler.handle(command(), DEVICE_INFO);

            assertThat(response.refreshToken()).isEqualTo(NEW_REFRESH);
            verify(sessionService).createSession(any(), eq(DEVICE_INFO));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  6. Edge Case — threshold boundary
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Boundary conditions")
    class Boundary {

        @Test
        @DisplayName("should rotate when remaining equals exactly threshold")
        void exactlyAtThreshold() {
            stubJwtValid();
            // remaining == THRESHOLD → nên rotate (≤ threshold)
            UserSession session = UserSession.reconstruct(
                    SessionId.of(104L), USER_ID, TOKEN_HASH,
                    DeviceInfo.of("Chrome", DevicePlatform.WEB, "127.0.0.1"),
                    FIXED_NOW.plusSeconds(THRESHOLD), // remaining == threshold
                    FIXED_NOW, FIXED_NOW, null
            );
            when(sessionRepository.findByTokenHash(TOKEN_HASH))
                    .thenReturn(Optional.of(session));
            when(userRepository.findById(USER_ID.value()))
                    .thenReturn(Optional.of(buildUser(UserStatus.ACTIVE)));
            when(sessionService.createSession(any(), any())).thenReturn(new SessionResult(new SessionId(999L), NEW_REFRESH));
            when(jwtPort.generateAccessToken(any(), any(), any(), any())).thenReturn(NEW_ACCESS);
            stubThreshold();

            AuthResponse response = handler.handle(command(), DEVICE_INFO);

            assertThat(response.refreshToken()).isEqualTo(NEW_REFRESH);
            verify(sessionRepository).revokeByTokenHash(TOKEN_HASH);
        }

        @Test
        @DisplayName("should renew (not rotate) when remaining is threshold + 1 second")
        void justAboveThreshold() {
            stubJwtValid();
            UserSession session = UserSession.reconstruct(
                    SessionId.of(105L), USER_ID, TOKEN_HASH,
                    DeviceInfo.of("Chrome", DevicePlatform.WEB, "127.0.0.1"),
                    FIXED_NOW.plusSeconds(THRESHOLD + 1),
                    FIXED_NOW, FIXED_NOW, null
            );
            when(sessionRepository.findByTokenHash(TOKEN_HASH))
                    .thenReturn(Optional.of(session));
            when(userRepository.findById(USER_ID.value()))
                    .thenReturn(Optional.of(buildUser(UserStatus.ACTIVE)));
            when(jwtPort.generateAccessToken(any(), any(), any(), any())).thenReturn(NEW_ACCESS);
            stubThreshold();
            when(sessionRepository.save(any(UserSession.class))).thenReturn(session);

            AuthResponse response = handler.handle(command(), DEVICE_INFO);

            assertThat(response.refreshToken()).isEqualTo(RAW_TOKEN); // unchanged
            verify(sessionRepository, never()).revokeByTokenHash(anyString());
        }
    }
}
