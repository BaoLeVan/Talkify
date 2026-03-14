package com.talkify.identity.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.security.crypto.password.PasswordEncoder;

import com.talkify.common.exception.AppException;
import com.talkify.common.exception.ErrorCode;
import com.talkify.identity.application.command.LoginCommand;
import com.talkify.identity.application.command.SendOtpCommand;
import com.talkify.identity.application.dto.response.AuthResponse;
import com.talkify.identity.application.port.JwtPort;
import com.talkify.identity.domain.model.Email;
import com.talkify.identity.domain.model.Password;
import com.talkify.identity.domain.model.User;
import com.talkify.identity.domain.model.UserId;
import com.talkify.identity.domain.model.Username;
import com.talkify.identity.domain.model.PhoneNumber;
import com.talkify.identity.domain.model.UserRole;
import com.talkify.identity.domain.model.UserStatus;
import com.talkify.identity.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginHandler")
class LoginHandlerTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private OtpHandler otpHandler;
    @Mock private JwtPort jwtPort;

    @InjectMocks private LoginHandler handler;

    private static final UserId USER_ID = UserId.of(1L);
    private static final String RAW_PASSWORD = "Abcdef12";
    private static final String HASHED = "$2a$10$hashed";

    private User buildUser(UserStatus status) {
        return User.reconstruct(
                USER_ID, "testuser", "Test User",
                Email.of("test@gmail.com"), "+84912345678",
                Password.ofHashed(HASHED),
                UserRole.USER, status,
                null, Instant.now(), null
        );
    }

    // ── Happy Case ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Happy Cases")
    class HappyCases {

        @Test
        @DisplayName("should login ACTIVE user by email and return JWT")
        void loginByEmail() {
            User user = buildUser(UserStatus.ACTIVE);
            when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED)).thenReturn(true);
            when(jwtPort.generateAccessToken(USER_ID, UserRole.USER, UserStatus.ACTIVE))
                    .thenReturn("access");
            when(jwtPort.generateRefreshToken(USER_ID)).thenReturn("refresh");

            AuthResponse response = handler.handle(new LoginCommand("test@gmail.com", RAW_PASSWORD));

            assertThat(response.accessToken()).isEqualTo("access");
            assertThat(response.refreshToken()).isEqualTo("refresh");
            assertThat(response.user().status()).isEqualTo("ACTIVE");
            assertThat(response.user().email()).isEqualTo("test@gmail.com");
        }

        @Test
        @DisplayName("should login by username")
        void loginByUsername() {
            User user = buildUser(UserStatus.ACTIVE);
            when(userRepository.findByUsername(any(Username.class))).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED)).thenReturn(true);
            when(jwtPort.generateAccessToken(any(), any(), any())).thenReturn("t");
            when(jwtPort.generateRefreshToken(any())).thenReturn("t");

            AuthResponse response = handler.handle(new LoginCommand("testuser", RAW_PASSWORD));

            assertThat(response.user().username()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("should login by phone number")
        void loginByPhone() {
            User user = buildUser(UserStatus.ACTIVE);
            when(userRepository.findByPhoneNumber(any(PhoneNumber.class))).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED)).thenReturn(true);
            when(jwtPort.generateAccessToken(any(), any(), any())).thenReturn("t");
            when(jwtPort.generateRefreshToken(any())).thenReturn("t");

            AuthResponse response = handler.handle(new LoginCommand("+84912345678", RAW_PASSWORD));

            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("should mask phone number in response")
        void maskPhone() {
            User user = buildUser(UserStatus.ACTIVE);
            when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED)).thenReturn(true);
            when(jwtPort.generateAccessToken(any(), any(), any())).thenReturn("t");
            when(jwtPort.generateRefreshToken(any())).thenReturn("t");

            AuthResponse response = handler.handle(new LoginCommand("test@gmail.com", RAW_PASSWORD));

            // +84912345678 (12 chars) → prefix 4 + stars 6 + suffix 2
            assertThat(response.user().phoneNumber()).isEqualTo("+849******78");
        }
    }

    // ── Bad Cases ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Bad Cases")
    class BadCases {

        @Test
        @DisplayName("should throw INVALID_CREDENTIALS when user not found")
        void userNotFound() {
            when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> handler.handle(new LoginCommand("x@gmail.com", RAW_PASSWORD)))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("should throw INVALID_CREDENTIALS when password wrong — same error as user not found")
        void wrongPassword() {
            User user = buildUser(UserStatus.ACTIVE);
            when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED)).thenReturn(false);

            assertThatThrownBy(() -> handler.handle(new LoginCommand("test@gmail.com", RAW_PASSWORD)))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

            verify(jwtPort, never()).generateAccessToken(any(), any(), any());
        }
    }

    // ── Status-Based Cases ───────────────────────────────────────────────

    @Nested
    @DisplayName("Login with different UserStatus")
    class StatusCases {

        @Test
        @DisplayName("INACTIVE → return JWT with INACTIVE status + send OTP")
        void inactiveUser() {
            User user = buildUser(UserStatus.INACTIVE);
            when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED)).thenReturn(true);
            when(jwtPort.generateAccessToken(USER_ID, UserRole.USER, UserStatus.INACTIVE))
                    .thenReturn("inactive-token");
            when(jwtPort.generateRefreshToken(USER_ID)).thenReturn("refresh");

            AuthResponse response = handler.handle(new LoginCommand("test@gmail.com", RAW_PASSWORD));

            assertThat(response.accessToken()).isEqualTo("inactive-token");
            assertThat(response.user().status()).isEqualTo("INACTIVE");
            verify(otpHandler).handle(any(SendOtpCommand.class));  // OTP was sent
        }

        @Test
        @DisplayName("BANNED → throw USER_BANNED")
        void bannedUser() {
            User user = buildUser(UserStatus.BANNED);
            when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED)).thenReturn(true);

            assertThatThrownBy(() -> handler.handle(new LoginCommand("test@gmail.com", RAW_PASSWORD)))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.USER_BANNED);

            verify(jwtPort, never()).generateAccessToken(any(), any(), any());
        }

        @Test
        @DisplayName("DELETED → throw INVALID_CREDENTIALS — same as user not found")
        void deletedUser() {
            User user = buildUser(UserStatus.DELETED);
            when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED)).thenReturn(true);

            assertThatThrownBy(() -> handler.handle(new LoginCommand("test@gmail.com", RAW_PASSWORD)))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    // ── Rare / Concurrency Conditions ────────────────────────────────────

    @Nested
    @DisplayName("Rare Conditions")
    class RareConditions {

        @Test
        @DisplayName("INACTIVE login — if OTP send fails, should propagate exception (no JWT)")
        void otpSendFailsDuringInactiveLogin() {
            User user = buildUser(UserStatus.INACTIVE);
            when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED)).thenReturn(true);
            doThrow(new RuntimeException("Email service down"))
                    .when(otpHandler).handle(any(SendOtpCommand.class));

            assertThatThrownBy(() -> handler.handle(new LoginCommand("test@gmail.com", RAW_PASSWORD)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Email service down");

            verify(jwtPort, never()).generateAccessToken(any(), any(), any());
        }

        @Test
        @DisplayName("JWT generation fails — should propagate exception")
        void jwtGenerationFails() {
            User user = buildUser(UserStatus.ACTIVE);
            when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED)).thenReturn(true);
            when(jwtPort.generateAccessToken(any(), any(), any()))
                    .thenThrow(new RuntimeException("Key not configured"));

            assertThatThrownBy(() -> handler.handle(new LoginCommand("test@gmail.com", RAW_PASSWORD)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Key not configured");
        }

        @Test
        @DisplayName("should return null phoneNumber when user has no phone")
        void noPhone() {
            User user = User.reconstruct(
                    USER_ID, "testuser", "Test",
                    Email.of("test@gmail.com"), null,
                    Password.ofHashed(HASHED),
                    UserRole.USER, UserStatus.ACTIVE,
                    null, Instant.now(), null
            );
            when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED)).thenReturn(true);
            when(jwtPort.generateAccessToken(any(), any(), any())).thenReturn("t");
            when(jwtPort.generateRefreshToken(any())).thenReturn("t");

            AuthResponse response = handler.handle(new LoginCommand("test@gmail.com", RAW_PASSWORD));

            assertThat(response.user().phoneNumber()).isNull();
        }
    }
}
