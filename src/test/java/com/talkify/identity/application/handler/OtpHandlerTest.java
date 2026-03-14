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

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.talkify.common.exception.AppException;
import com.talkify.common.exception.ErrorCode;
import com.talkify.identity.application.command.ResendOtpCommand;
import com.talkify.identity.application.command.SendOtpCommand;
import com.talkify.identity.application.command.VerifyOtpCommand;
import com.talkify.identity.application.port.CachePort;
import com.talkify.identity.application.port.EmailPort;
import com.talkify.identity.application.port.OtpGeneratorPort;
import com.talkify.identity.domain.model.Email;
import com.talkify.identity.domain.model.OtpCacheKey;
import com.talkify.identity.domain.model.OtpPurpose;
import com.talkify.identity.domain.model.Password;
import com.talkify.identity.domain.model.User;
import com.talkify.identity.domain.model.UserId;
import com.talkify.identity.domain.model.UserRole;
import com.talkify.identity.domain.model.UserStatus;
import com.talkify.identity.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("OtpHandler")
class OtpHandlerTest {

    @Mock private UserRepository userRepository;
    @Mock private CachePort cachePort;
    @Mock private EmailPort emailPort;
    @Mock private OtpGeneratorPort otpGeneratorPort;

    @InjectMocks private OtpHandler handler;

    private static final UserId USER_ID = UserId.of(1L);
    private static final String HASHED = "$2a$10$hashed";
    private static final String OTP_CODE = "123456";
    private static final String EMAIL = "test@gmail.com";

    private User buildUser(UserStatus status) {
        return User.reconstruct(
                USER_ID, "testuser", "Test User",
                Email.of(EMAIL), "+84912345678",
                Password.ofHashed(HASHED),
                UserRole.USER, status,
                null, Instant.now(), null
        );
    }

    /** Cache key derived from userId + purpose */
    private String otpKey(OtpPurpose purpose) {
        return OtpCacheKey.of(USER_ID, purpose).value();
    }

    private String attemptKey(OtpPurpose purpose) {
        return OtpCacheKey.attemptOf(USER_ID, purpose).value();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  SendOtp
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("SendOtp")
    class SendOtpTests {

        @Test
        @DisplayName("should generate code, cache, reset attempts, and send email")
        void happyPath() {
            User user = buildUser(UserStatus.INACTIVE);
            when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
            when(otpGeneratorPort.generateCode()).thenReturn(OTP_CODE);

            handler.handle(new SendOtpCommand(EMAIL, OtpPurpose.REGISTRATION));

            verify(cachePort).set(eq(otpKey(OtpPurpose.REGISTRATION)), eq(OTP_CODE), eq(Duration.ofMinutes(5)));
            verify(cachePort).delete(attemptKey(OtpPurpose.REGISTRATION));  // reset attempt counter
            verify(emailPort).sendVerificationEmail(EMAIL, "Test User", OTP_CODE);
        }

        @Test
        @DisplayName("should throw USER_NOT_FOUND when email does not exist")
        void userNotFound() {
            when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> handler.handle(new SendOtpCommand(EMAIL, OtpPurpose.REGISTRATION)))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);

            verify(cachePort, never()).set(anyString(), anyString(), any());
            verify(emailPort, never()).sendVerificationEmail(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("should propagate exception when email service fails")
        void emailServiceDown() {
            User user = buildUser(UserStatus.INACTIVE);
            when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
            when(otpGeneratorPort.generateCode()).thenReturn(OTP_CODE);
            doThrow(new RuntimeException("SMTP connection refused"))
                    .when(emailPort).sendVerificationEmail(anyString(), anyString(), anyString());

            assertThatThrownBy(() -> handler.handle(new SendOtpCommand(EMAIL, OtpPurpose.REGISTRATION)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("SMTP connection refused");
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  ResendOtp
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("ResendOtp")
    class ResendOtpTests {

        @Test
        @DisplayName("should resend OTP when cooldown elapsed (ttl below threshold)")
        void happyPath() {
            User user = buildUser(UserStatus.INACTIVE);
            when(userRepository.findById(USER_ID.value())).thenReturn(Optional.of(user));
            // TTL = 180s → means 120s elapsed already, so cooldown (60s) passed
            when(cachePort.getExpire(otpKey(OtpPurpose.REGISTRATION))).thenReturn(180L);
            when(otpGeneratorPort.generateCode()).thenReturn(OTP_CODE);

            handler.handle(new ResendOtpCommand(OtpPurpose.REGISTRATION), USER_ID);

            verify(cachePort).set(eq(otpKey(OtpPurpose.REGISTRATION)), eq(OTP_CODE), eq(Duration.ofMinutes(5)));
            verify(cachePort).delete(attemptKey(OtpPurpose.REGISTRATION));
            verify(emailPort).sendVerificationEmail(EMAIL, "Test User", OTP_CODE);
        }

        @Test
        @DisplayName("should resend when no previous OTP exists (ttl = null)")
        void noPreviousOtp() {
            User user = buildUser(UserStatus.INACTIVE);
            when(userRepository.findById(USER_ID.value())).thenReturn(Optional.of(user));
            when(cachePort.getExpire(otpKey(OtpPurpose.REGISTRATION))).thenReturn(null);
            when(otpGeneratorPort.generateCode()).thenReturn(OTP_CODE);

            handler.handle(new ResendOtpCommand(OtpPurpose.REGISTRATION), USER_ID);

            verify(emailPort).sendVerificationEmail(EMAIL, "Test User", OTP_CODE);
        }

        @Test
        @DisplayName("should throw OTP_RESEND_TOO_FREQUENT when TTL >= 240s (cooldown not elapsed)")
        void tooFrequent() {
            User user = buildUser(UserStatus.INACTIVE);
            when(userRepository.findById(USER_ID.value())).thenReturn(Optional.of(user));
            // OTP_TTL=300s, RESEND_COOLDOWN=60s → threshold = 300-60 = 240
            // TTL=280s means only 20s since sent → too frequent
            when(cachePort.getExpire(otpKey(OtpPurpose.REGISTRATION))).thenReturn(280L);

            assertThatThrownBy(() -> handler.handle(new ResendOtpCommand(OtpPurpose.REGISTRATION), USER_ID))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.OTP_RESEND_TOO_FREQUENT);

            verify(emailPort, never()).sendVerificationEmail(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("should throw USER_ALREADY_ACTIVE for REGISTRATION + ACTIVE user")
        void alreadyActive() {
            User user = buildUser(UserStatus.ACTIVE);
            when(userRepository.findById(USER_ID.value())).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> handler.handle(new ResendOtpCommand(OtpPurpose.REGISTRATION), USER_ID))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.USER_ALREADY_ACTIVE);
        }

        @Test
        @DisplayName("should throw USER_NOT_VERIFIED for PASSWORD_RESET + INACTIVE user")
        void passwordResetInactive() {
            User user = buildUser(UserStatus.INACTIVE);
            when(userRepository.findById(USER_ID.value())).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> handler.handle(new ResendOtpCommand(OtpPurpose.PASSWORD_RESET), USER_ID))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.USER_NOT_VERIFIED);
        }

        @Test
        @DisplayName("should throw USER_NOT_FOUND when userId does not exist")
        void userNotFound() {
            when(userRepository.findById(USER_ID.value())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> handler.handle(new ResendOtpCommand(OtpPurpose.REGISTRATION), USER_ID))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  VerifyOtp
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("VerifyOtp")
    class VerifyOtpTests {

        @Test
        @DisplayName("should verify OTP, activate user, save, and clear attempt counter")
        void happyPath_registration() {
            User user = buildUser(UserStatus.INACTIVE);
            when(userRepository.findById(USER_ID.value())).thenReturn(Optional.of(user));
            when(cachePort.increment(eq(attemptKey(OtpPurpose.REGISTRATION)), any())).thenReturn(1L);
            when(cachePort.get(otpKey(OtpPurpose.REGISTRATION))).thenReturn(Optional.of(OTP_CODE));
            when(cachePort.getAndDelete(otpKey(OtpPurpose.REGISTRATION))).thenReturn(Optional.of(OTP_CODE));

            handler.handle(new VerifyOtpCommand(OTP_CODE, OtpPurpose.REGISTRATION), USER_ID);

            // User was activated (REGISTRATION.applyEffect)
            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
            verify(userRepository).save(user);
            verify(cachePort).delete(attemptKey(OtpPurpose.REGISTRATION));
        }

        @Test
        @DisplayName("should verify OTP for PASSWORD_RESET without status change")
        void happyPath_passwordReset() {
            User user = buildUser(UserStatus.ACTIVE);
            when(userRepository.findById(USER_ID.value())).thenReturn(Optional.of(user));
            when(cachePort.increment(eq(attemptKey(OtpPurpose.PASSWORD_RESET)), any())).thenReturn(1L);
            when(cachePort.get(otpKey(OtpPurpose.PASSWORD_RESET))).thenReturn(Optional.of(OTP_CODE));
            when(cachePort.getAndDelete(otpKey(OtpPurpose.PASSWORD_RESET))).thenReturn(Optional.of(OTP_CODE));

            handler.handle(new VerifyOtpCommand(OTP_CODE, OtpPurpose.PASSWORD_RESET), USER_ID);

            // Status unchanged for PASSWORD_RESET
            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("should throw OTP_INVALID when wrong code, OTP stays in cache")
        void wrongOtp() {
            User user = buildUser(UserStatus.INACTIVE);
            when(userRepository.findById(USER_ID.value())).thenReturn(Optional.of(user));
            when(cachePort.increment(eq(attemptKey(OtpPurpose.REGISTRATION)), any())).thenReturn(1L);
            when(cachePort.get(otpKey(OtpPurpose.REGISTRATION))).thenReturn(Optional.of(OTP_CODE));

            assertThatThrownBy(() -> handler.handle(
                    new VerifyOtpCommand("000000", OtpPurpose.REGISTRATION), USER_ID))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.OTP_INVALID);

            // OTP NOT deleted — user can retry
            verify(cachePort, never()).getAndDelete(anyString());
            verify(cachePort, never()).delete(otpKey(OtpPurpose.REGISTRATION));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw OTP_EXPIRED when no cached OTP exists")
        void otpExpired() {
            User user = buildUser(UserStatus.INACTIVE);
            when(userRepository.findById(USER_ID.value())).thenReturn(Optional.of(user));
            when(cachePort.increment(eq(attemptKey(OtpPurpose.REGISTRATION)), any())).thenReturn(1L);
            when(cachePort.get(otpKey(OtpPurpose.REGISTRATION))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> handler.handle(
                    new VerifyOtpCommand(OTP_CODE, OtpPurpose.REGISTRATION), USER_ID))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.OTP_EXPIRED);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should lock out and delete OTP after MAX_ATTEMPTS exceeded")
        void bruteForce() {
            User user = buildUser(UserStatus.INACTIVE);
            when(userRepository.findById(USER_ID.value())).thenReturn(Optional.of(user));
            // 6th attempt → over MAX_ATTEMPTS (5)
            when(cachePort.increment(eq(attemptKey(OtpPurpose.REGISTRATION)), any())).thenReturn(6L);

            assertThatThrownBy(() -> handler.handle(
                    new VerifyOtpCommand(OTP_CODE, OtpPurpose.REGISTRATION), USER_ID))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.OTP_TOO_MANY_ATTEMPTS);

            // OTP is deleted on brute-force lockout
            verify(cachePort).delete(otpKey(OtpPurpose.REGISTRATION));
            // Never reads the OTP
            verify(cachePort, never()).get(otpKey(OtpPurpose.REGISTRATION));
        }

        @Test
        @DisplayName("should throw USER_ALREADY_ACTIVE when verifying REGISTRATION OTP for ACTIVE user")
        void alreadyActive() {
            User user = buildUser(UserStatus.ACTIVE);
            when(userRepository.findById(USER_ID.value())).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> handler.handle(
                    new VerifyOtpCommand(OTP_CODE, OtpPurpose.REGISTRATION), USER_ID))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.USER_ALREADY_ACTIVE);

            // No Redis activity happened
            verify(cachePort, never()).increment(anyString(), any());
        }

        @Test
        @DisplayName("should throw USER_NOT_VERIFIED for PASSWORD_RESET + INACTIVE user")
        void passwordResetNotVerified() {
            User user = buildUser(UserStatus.INACTIVE);
            when(userRepository.findById(USER_ID.value())).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> handler.handle(
                    new VerifyOtpCommand(OTP_CODE, OtpPurpose.PASSWORD_RESET), USER_ID))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.USER_NOT_VERIFIED);
        }

        @Test
        @DisplayName("concurrent: getAndDelete returns empty after get succeeded → OTP_EXPIRED")
        void concurrentConsume() {
            User user = buildUser(UserStatus.INACTIVE);
            when(userRepository.findById(USER_ID.value())).thenReturn(Optional.of(user));
            when(cachePort.increment(eq(attemptKey(OtpPurpose.REGISTRATION)), any())).thenReturn(1L);
            when(cachePort.get(otpKey(OtpPurpose.REGISTRATION))).thenReturn(Optional.of(OTP_CODE));
            // Simulates race: another request consumed the OTP between get() and getAndDelete()
            when(cachePort.getAndDelete(otpKey(OtpPurpose.REGISTRATION))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> handler.handle(
                    new VerifyOtpCommand(OTP_CODE, OtpPurpose.REGISTRATION), USER_ID))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.OTP_EXPIRED);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("concurrent: getAndDelete returns different code → OTP_EXPIRED")
        void concurrentDifferentCode() {
            User user = buildUser(UserStatus.INACTIVE);
            when(userRepository.findById(USER_ID.value())).thenReturn(Optional.of(user));
            when(cachePort.increment(eq(attemptKey(OtpPurpose.REGISTRATION)), any())).thenReturn(1L);
            when(cachePort.get(otpKey(OtpPurpose.REGISTRATION))).thenReturn(Optional.of(OTP_CODE));
            // Between get() and getAndDelete(), a resend wrote a new OTP code
            when(cachePort.getAndDelete(otpKey(OtpPurpose.REGISTRATION))).thenReturn(Optional.of("999999"));

            assertThatThrownBy(() -> handler.handle(
                    new VerifyOtpCommand(OTP_CODE, OtpPurpose.REGISTRATION), USER_ID))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.OTP_EXPIRED);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw USER_NOT_FOUND when userId does not exist")
        void userNotFound() {
            when(userRepository.findById(USER_ID.value())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> handler.handle(
                    new VerifyOtpCommand(OTP_CODE, OtpPurpose.REGISTRATION), USER_ID))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("db save fails after verify → exception propagated, OTP already consumed")
        void dbSaveFailsAfterVerify() {
            User user = buildUser(UserStatus.INACTIVE);
            when(userRepository.findById(USER_ID.value())).thenReturn(Optional.of(user));
            when(cachePort.increment(eq(attemptKey(OtpPurpose.REGISTRATION)), any())).thenReturn(1L);
            when(cachePort.get(otpKey(OtpPurpose.REGISTRATION))).thenReturn(Optional.of(OTP_CODE));
            when(cachePort.getAndDelete(otpKey(OtpPurpose.REGISTRATION))).thenReturn(Optional.of(OTP_CODE));
            when(userRepository.save(any())).thenThrow(new RuntimeException("DB write timeout"));

            assertThatThrownBy(() -> handler.handle(
                    new VerifyOtpCommand(OTP_CODE, OtpPurpose.REGISTRATION), USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB write timeout");

            // User was activated in memory, but DB save failed
            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        }
    }
}
