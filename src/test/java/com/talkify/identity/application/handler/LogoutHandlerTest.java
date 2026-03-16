package com.talkify.identity.application.handler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.talkify.common.util.Sha256Utils;
import com.talkify.identity.application.command.LogoutCommand;
import com.talkify.identity.domain.model.LogoutScope;
import com.talkify.identity.domain.model.UserId;
import com.talkify.identity.domain.repository.SessionRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("LogoutHandler")
class LogoutHandlerTest {

    @Mock  private SessionRepository sessionRepository;
    @InjectMocks private LogoutHandler handler;

    // ── Constants ────────────────────────────────────────────────────────────
    private static final UserId USER_ID   = UserId.of(1L);
    private static final String RAW_TOKEN = "valid-raw-refresh-token";
    private static final String TOKEN_HASH = Sha256Utils.hash(RAW_TOKEN);

    // ── Helpers ───────────────────────────────────────────────────────────────
    private LogoutCommand command(String rawToken, LogoutScope scope) {
        return new LogoutCommand(rawToken, scope);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  1. CURRENT_SESSION_ONLY
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("CURRENT_SESSION_ONLY")
    class CurrentSessionOnly {

        @Test
        @DisplayName("should revoke only the current session by token hash")
        void happyPath_revokesCurrentSession() {
            handler.handle(command(RAW_TOKEN, LogoutScope.CURRENT_SESSION_ONLY), USER_ID);

            verify(sessionRepository).revokeByTokenHash(TOKEN_HASH);
            verify(sessionRepository, never()).revokeAllByUserId(any());
            verify(sessionRepository, never()).revokeAllByUserIdExceptTokenHash(any(), anyString());
        }

        @Test
        @DisplayName("should hash the raw token before passing to repository")
        void hashesTokenBeforePersistence() {
            // Verify the adapter never sees raw token — only its SHA-256 hash
            handler.handle(command(RAW_TOKEN, LogoutScope.CURRENT_SESSION_ONLY), USER_ID);

            verify(sessionRepository).revokeByTokenHash(eq(TOKEN_HASH));
            verify(sessionRepository, never()).revokeByTokenHash(eq(RAW_TOKEN));
        }

        @Test
        @DisplayName("should be idempotent when RT cookie is absent (null)")
        void noToken_null_doesNothing() {
            handler.handle(command(null, LogoutScope.CURRENT_SESSION_ONLY), USER_ID);

            verifyNoInteractions(sessionRepository);
        }

        @Test
        @DisplayName("should be idempotent when RT cookie is empty string")
        void noToken_emptyString_doesNothing() {
            handler.handle(command("", LogoutScope.CURRENT_SESSION_ONLY), USER_ID);

            verifyNoInteractions(sessionRepository);
        }

        @Test
        @DisplayName("should be idempotent when RT cookie is whitespace-only")
        void noToken_whitespace_doesNothing() {
            handler.handle(command("   ", LogoutScope.CURRENT_SESSION_ONLY), USER_ID);

            verifyNoInteractions(sessionRepository);
        }

        @Test
        @DisplayName("should NOT throw even if session was already revoked before (idempotent)")
        void alreadyRevoked_doesNotThrow() {
            // Repository's revokeByTokenHash is a no-op for already-revoked sessions
            // → handler must not throw regardless
            assertThatCode(() ->
                handler.handle(command(RAW_TOKEN, LogoutScope.CURRENT_SESSION_ONLY), USER_ID)
            ).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should NOT use userId for CURRENT_SESSION_ONLY — avoids over-revoke")
        void doesNotUseUserId() {
            handler.handle(command(RAW_TOKEN, LogoutScope.CURRENT_SESSION_ONLY), USER_ID);

            verify(sessionRepository, never()).revokeAllByUserId(any());
            verify(sessionRepository, never()).revokeAllByUserIdExceptTokenHash(any(), anyString());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  2. ALL_EXCEPT_CURRENT
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("ALL_EXCEPT_CURRENT")
    class AllExceptCurrent {

        @Test
        @DisplayName("should revoke all sessions except the current one")
        void happyPath_revokesAllExceptCurrent() {
            handler.handle(command(RAW_TOKEN, LogoutScope.ALL_EXCEPT_CURRENT), USER_ID);

            verify(sessionRepository).revokeAllByUserIdExceptTokenHash(USER_ID, TOKEN_HASH);
            verify(sessionRepository, never()).revokeAllByUserId(any());
            verify(sessionRepository, never()).revokeByTokenHash(anyString());
        }

        @Test
        @DisplayName("should pass hashed token (not raw) to repository")
        void hashesToken() {
            handler.handle(command(RAW_TOKEN, LogoutScope.ALL_EXCEPT_CURRENT), USER_ID);

            verify(sessionRepository).revokeAllByUserIdExceptTokenHash(eq(USER_ID), eq(TOKEN_HASH));
            verify(sessionRepository, never()).revokeAllByUserIdExceptTokenHash(any(), eq(RAW_TOKEN));
        }

        @Test
        @DisplayName("should degrade to ALL_SESSIONS when RT cookie is absent (null)")
        void noToken_null_degradesToAllSessions() {
            handler.handle(command(null, LogoutScope.ALL_EXCEPT_CURRENT), USER_ID);

            verify(sessionRepository).revokeAllByUserId(USER_ID);
            verify(sessionRepository, never()).revokeAllByUserIdExceptTokenHash(any(), anyString());
        }

        @Test
        @DisplayName("should degrade to ALL_SESSIONS when RT cookie is empty string")
        void noToken_emptyString_degradesToAllSessions() {
            handler.handle(command("", LogoutScope.ALL_EXCEPT_CURRENT), USER_ID);

            verify(sessionRepository).revokeAllByUserId(USER_ID);
        }

        @Test
        @DisplayName("should degrade to ALL_SESSIONS when RT cookie is whitespace-only")
        void noToken_whitespace_degradesToAllSessions() {
            handler.handle(command("   ", LogoutScope.ALL_EXCEPT_CURRENT), USER_ID);

            verify(sessionRepository).revokeAllByUserId(USER_ID);
        }

        @Test
        @DisplayName("should NOT revoke the current session when has token")
        void doesNotRevokeCurrentSession() {
            handler.handle(command(RAW_TOKEN, LogoutScope.ALL_EXCEPT_CURRENT), USER_ID);

            verify(sessionRepository, never()).revokeByTokenHash(anyString());
        }

        @Test
        @DisplayName("should scope revoke to correct userId — wrong user cannot be affected")
        void scopedToCorrectUserId() {
            UserId otherUserId = UserId.of(999L);
            handler.handle(command(RAW_TOKEN, LogoutScope.ALL_EXCEPT_CURRENT), USER_ID);

            // Only USER_ID was used, never otherUserId
            verify(sessionRepository).revokeAllByUserIdExceptTokenHash(eq(USER_ID), anyString());
            verify(sessionRepository, never()).revokeAllByUserIdExceptTokenHash(eq(otherUserId), anyString());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  3. ALL_SESSIONS
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("ALL_SESSIONS")
    class AllSessions {

        @Test
        @DisplayName("should revoke all sessions — RT cookie not required")
        void happyPath_revokesAll() {
            handler.handle(command(RAW_TOKEN, LogoutScope.ALL_SESSIONS), USER_ID);

            verify(sessionRepository).revokeAllByUserId(USER_ID);
            verify(sessionRepository, never()).revokeByTokenHash(anyString());
            verify(sessionRepository, never()).revokeAllByUserIdExceptTokenHash(any(), anyString());
        }

        @Test
        @DisplayName("should revoke all sessions even when RT cookie is absent")
        void noToken_stillRevokesAll() {
            handler.handle(command(null, LogoutScope.ALL_SESSIONS), USER_ID);

            verify(sessionRepository).revokeAllByUserId(USER_ID);
        }

        @Test
        @DisplayName("should revoke all sessions when RT is empty string")
        void emptyToken_stillRevokesAll() {
            handler.handle(command("", LogoutScope.ALL_SESSIONS), USER_ID);

            verify(sessionRepository).revokeAllByUserId(USER_ID);
        }

        @Test
        @DisplayName("should scope revoke to the correct userId")
        void scopedToCorrectUserId() {
            UserId anotherUser = UserId.of(42L);
            handler.handle(command(RAW_TOKEN, LogoutScope.ALL_SESSIONS), USER_ID);

            verify(sessionRepository).revokeAllByUserId(USER_ID);
            verify(sessionRepository, never()).revokeAllByUserId(anotherUser);
        }

        @Test
        @DisplayName("should not care about token hash — does not compute hash for ALL_SESSIONS")
        void doesNotUseTokenHash() {
            handler.handle(command(RAW_TOKEN, LogoutScope.ALL_SESSIONS), USER_ID);

            verify(sessionRepository, never()).revokeAllByUserIdExceptTokenHash(any(), anyString());
            verify(sessionRepository, never()).revokeByTokenHash(anyString());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  4. Idempotency — double logout / already-revoked
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Idempotency")
    class Idempotency {

        @Test
        @DisplayName("calling logout twice with CURRENT_SESSION_ONLY should not throw")
        void doubleLogout_currentOnly_noException() {
            assertThatCode(() -> {
                handler.handle(command(RAW_TOKEN, LogoutScope.CURRENT_SESSION_ONLY), USER_ID);
                handler.handle(command(RAW_TOKEN, LogoutScope.CURRENT_SESSION_ONLY), USER_ID);
            }).doesNotThrowAnyException();

            // Repository called twice — both calls are safe at DB level (revoked_at already set)
            verify(sessionRepository, times(2)).revokeByTokenHash(TOKEN_HASH);
        }

        @Test
        @DisplayName("calling logout twice with ALL_SESSIONS should not throw")
        void doubleLogout_allSessions_noException() {
            assertThatCode(() -> {
                handler.handle(command(null, LogoutScope.ALL_SESSIONS), USER_ID);
                handler.handle(command(null, LogoutScope.ALL_SESSIONS), USER_ID);
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("logout with no cookie should always succeed without any repo call — CURRENT_ONLY")
        void noCookie_currentOnly_noRepoInteraction() {
            assertThatCode(() ->
                handler.handle(command(null, LogoutScope.CURRENT_SESSION_ONLY), USER_ID)
            ).doesNotThrowAnyException();

            verifyNoInteractions(sessionRepository);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  5. Security — token isolation
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Security — token isolation")
    class TokenIsolation {

        @Test
        @DisplayName("two different tokens produce different hashes — sessions are isolated")
        void differentTokens_differentHashes() {
            String token1 = "token-device-A";
            String token2 = "token-device-B";

            handler.handle(command(token1, LogoutScope.CURRENT_SESSION_ONLY), USER_ID);
            handler.handle(command(token2, LogoutScope.CURRENT_SESSION_ONLY), USER_ID);

            verify(sessionRepository).revokeByTokenHash(Sha256Utils.hash(token1));
            verify(sessionRepository).revokeByTokenHash(Sha256Utils.hash(token2));
            // Ensure hashes are indeed different
            verify(sessionRepository, never()).revokeByTokenHash(token1); // raw never passed
            verify(sessionRepository, never()).revokeByTokenHash(token2); // raw never passed
        }

        @Test
        @DisplayName("ALL_EXCEPT_CURRENT uses userId from parameter — not from token claims")
        void userIdComesfromParameter_notTokenClaims() {
            UserId correctUser = UserId.of(10L);
            UserId wrongUser   = UserId.of(99L);

            handler.handle(command(RAW_TOKEN, LogoutScope.ALL_EXCEPT_CURRENT), correctUser);

            verify(sessionRepository).revokeAllByUserIdExceptTokenHash(eq(correctUser), anyString());
            verify(sessionRepository, never()).revokeAllByUserIdExceptTokenHash(eq(wrongUser), anyString());
        }

        @Test
        @DisplayName("ALL_SESSIONS uses userId from parameter — cannot revoke another user's sessions")
        void allSessions_onlyRevokesCorrectUser() {
            UserId victimUser = UserId.of(7L);

            handler.handle(command(null, LogoutScope.ALL_SESSIONS), USER_ID);

            verify(sessionRepository).revokeAllByUserId(USER_ID);
            verify(sessionRepository, never()).revokeAllByUserId(victimUser);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  6. Repository method exclusivity — no cross-calls
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Repository method exclusivity")
    class RepositoryExclusivity {

        @Test
        @DisplayName("CURRENT_SESSION_ONLY calls exactly one repo method")
        void currentOnly_exactlyOneRepoMethod() {
            handler.handle(command(RAW_TOKEN, LogoutScope.CURRENT_SESSION_ONLY), USER_ID);

            verify(sessionRepository).revokeByTokenHash(anyString());
            verifyNoMoreInteractions(sessionRepository);
        }

        @Test
        @DisplayName("ALL_EXCEPT_CURRENT (with token) calls exactly one repo method")
        void allExceptCurrent_withToken_exactlyOneRepoMethod() {
            handler.handle(command(RAW_TOKEN, LogoutScope.ALL_EXCEPT_CURRENT), USER_ID);

            verify(sessionRepository).revokeAllByUserIdExceptTokenHash(any(), anyString());
            verifyNoMoreInteractions(sessionRepository);
        }

        @Test
        @DisplayName("ALL_EXCEPT_CURRENT (without token) calls exactly one repo method")
        void allExceptCurrent_noToken_exactlyOneRepoMethod() {
            handler.handle(command(null, LogoutScope.ALL_EXCEPT_CURRENT), USER_ID);

            verify(sessionRepository).revokeAllByUserId(any());
            verifyNoMoreInteractions(sessionRepository);
        }

        @Test
        @DisplayName("ALL_SESSIONS calls exactly one repo method")
        void allSessions_exactlyOneRepoMethod() {
            handler.handle(command(RAW_TOKEN, LogoutScope.ALL_SESSIONS), USER_ID);

            verify(sessionRepository).revokeAllByUserId(any());
            verifyNoMoreInteractions(sessionRepository);
        }
    }
}
