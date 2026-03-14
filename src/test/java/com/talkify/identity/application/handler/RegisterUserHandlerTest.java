package com.talkify.identity.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.talkify.common.exception.AppException;
import com.talkify.common.exception.ErrorCode;
import com.talkify.common.id.IdGenerator;
import com.talkify.identity.application.command.RegisterUserCommand;
import com.talkify.identity.application.dto.response.AuthResponse;
import com.talkify.identity.application.port.JwtPort;
import com.talkify.identity.domain.event.UserRegisteredEvent;
import com.talkify.identity.domain.model.Email;
import com.talkify.identity.domain.model.OtpPurpose;
import com.talkify.identity.domain.model.User;
import com.talkify.identity.domain.model.UserRole;
import com.talkify.identity.domain.model.UserStatus;
import com.talkify.identity.domain.model.Username;
import com.talkify.identity.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterUserHandler")
class RegisterUserHandlerTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private IdGenerator idGenerator;
    @Mock private JwtPort jwtPort;

    @InjectMocks private RegisterUserHandler handler;

    private RegisterUserCommand validCommand;

    @BeforeEach
    void setUp() {
        validCommand = new RegisterUserCommand(
                "test@gmail.com", "testuser", "Abcdef12", "Abcdef12", "Test User"
        );
    }

    // ── Happy Case ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Happy Cases")
    class HappyCases {

        @Test
        @DisplayName("should register user and return AuthResponse with INACTIVE status")
        void registerSuccess() {
            when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
            when(userRepository.existsByUsername(any(Username.class))).thenReturn(false);
            when(passwordEncoder.encode("Abcdef12")).thenReturn("$2a$10$hashedpassword");
            when(idGenerator.nextId()).thenReturn(100L);
            when(jwtPort.generateAccessToken(any(), eq(UserRole.USER), eq(UserStatus.INACTIVE)))
                    .thenReturn("access-token");
            when(jwtPort.generateRefreshToken(any())).thenReturn("refresh-token");

            AuthResponse response = handler.handle(validCommand);

            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-token");
            assertThat(response.user().email()).isEqualTo("test@gmail.com");
            assertThat(response.user().status()).isEqualTo("INACTIVE");
            assertThat(response.user().role()).isEqualTo("USER");
            assertThat(response.user().phoneNumber()).isNull();

            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("should publish UserRegisteredEvent after save")
        void publishesEvent() {
            when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
            when(userRepository.existsByUsername(any(Username.class))).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$hashed");
            when(idGenerator.nextId()).thenReturn(100L);
            when(jwtPort.generateAccessToken(any(), any(), any())).thenReturn("t");
            when(jwtPort.generateRefreshToken(any())).thenReturn("t");

            handler.handle(validCommand);

            ArgumentCaptor<UserRegisteredEvent> captor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());

            UserRegisteredEvent event = captor.getValue();
            assertThat(event.email()).isEqualTo("test@gmail.com");
            assertThat(event.otpPurpose()).isEqualTo(OtpPurpose.REGISTRATION);
        }

        @Test
        @DisplayName("should hash password before saving user")
        void hashesPassword() {
            when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
            when(userRepository.existsByUsername(any(Username.class))).thenReturn(false);
            when(passwordEncoder.encode("Abcdef12")).thenReturn("$2a$hashed");
            when(idGenerator.nextId()).thenReturn(100L);
            when(jwtPort.generateAccessToken(any(), any(), any())).thenReturn("t");
            when(jwtPort.generateRefreshToken(any())).thenReturn("t");

            handler.handle(validCommand);

            verify(passwordEncoder).encode("Abcdef12");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPassword().value()).isEqualTo("$2a$hashed");
        }
    }

    // ── Bad Cases ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Bad Cases")
    class BadCases {

        @Test
        @DisplayName("should throw EMAIL_ALREADY_EXISTS when email is taken")
        void emailAlreadyExists() {
            when(userRepository.existsByEmail(any(Email.class))).thenReturn(true);

            assertThatThrownBy(() -> handler.handle(validCommand))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);

            verify(userRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("should throw USERNAME_ALREADY_EXISTS when username is taken")
        void usernameAlreadyExists() {
            when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
            when(userRepository.existsByUsername(any(Username.class))).thenReturn(true);

            assertThatThrownBy(() -> handler.handle(validCommand))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.USERNAME_ALREADY_EXISTS);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw PASSWORD_TOO_WEAK when password fails validation")
        void weakPassword() {
            RegisterUserCommand weak = new RegisterUserCommand(
                    "test@gmail.com", "testuser", "short", "short", "Test"
            );
            when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
            when(userRepository.existsByUsername(any(Username.class))).thenReturn(false);

            assertThatThrownBy(() -> handler.handle(weak))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.PASSWORD_TOO_WEAK);

            verify(passwordEncoder, never()).encode(anyString());
            verify(userRepository, never()).save(any());
        }
    }

    // ── Edge Cases ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Edge Cases & Rare Conditions")
    class EdgeCases {

        @Test
        @DisplayName("should not publish event when save throws exception")
        void saveFailsNoEvent() {
            when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
            when(userRepository.existsByUsername(any(Username.class))).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$hashed");
            when(idGenerator.nextId()).thenReturn(100L);
            when(userRepository.save(any(User.class)))
                    .thenThrow(new RuntimeException("DB connection lost"));

            assertThatThrownBy(() -> handler.handle(validCommand))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB connection lost");

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("should generate unique userId via IdGenerator for each call")
        void usesIdGenerator() {
            when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
            when(userRepository.existsByUsername(any(Username.class))).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$hashed");
            when(idGenerator.nextId()).thenReturn(999L);
            when(jwtPort.generateAccessToken(any(), any(), any())).thenReturn("t");
            when(jwtPort.generateRefreshToken(any())).thenReturn("t");

            AuthResponse response = handler.handle(validCommand);

            assertThat(response.user().id()).isEqualTo(999L);
            verify(idGenerator).nextId();
        }

        @Test
        @DisplayName("should save user before generating JWT — JWT failure should not lose user data")
        void saveBeforeJwt() {
            when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
            when(userRepository.existsByUsername(any(Username.class))).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$hashed");
            when(idGenerator.nextId()).thenReturn(100L);
            when(jwtPort.generateAccessToken(any(), any(), any()))
                    .thenThrow(new RuntimeException("JWT signing error"));

            assertThatThrownBy(() -> handler.handle(validCommand))
                    .isInstanceOf(RuntimeException.class);

            // User was already saved before JWT generation
            verify(userRepository).save(any(User.class));
        }
    }
}
