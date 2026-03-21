package com.talkify.identity.application.handler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.talkify.identity.application.command.LogoutCommand;
import com.talkify.identity.application.port.SessionCachePort;
import com.talkify.identity.domain.model.DeviceInfo;
import com.talkify.identity.domain.model.DevicePlatform;
import com.talkify.identity.domain.model.LogoutScope;
import com.talkify.identity.domain.model.SessionId;
import com.talkify.identity.domain.model.UserId;
import com.talkify.identity.domain.model.UserSession;
import com.talkify.identity.domain.repository.SessionRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("LogoutHandler")
class LogoutHandlerTest {

    @Mock private SessionRepository sessionRepository;
    @Mock private SessionCachePort  sessionCachePort;
    @InjectMocks private LogoutHandler handler;

    private static final UserId    USER_ID    = UserId.of(1L);
    private static final SessionId SESSION_ID = SessionId.of(100L);
    private static final Instant   EXPIRES_AT = Instant.now().plusSeconds(604800);

    private LogoutCommand command(LogoutScope scope) {
        return new LogoutCommand(SESSION_ID, scope);
    }

    private UserSession fakeSession() {
        return UserSession.reconstruct(
                SESSION_ID, USER_ID, "tokenHash",
                DeviceInfo.ofUnknown(DevicePlatform.WEB, "127.0.0.1"),
                EXPIRES_AT, Instant.now(), Instant.now(), null);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  1. CURRENT_SESSION_ONLY
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("CURRENT_SESSION_ONLY")
    class CurrentSessionOnly {

        @Test
        @DisplayName("should revoke session in DB then evict from cache")
        void happyPath() {
            handler.handle(command(LogoutScope.CURRENT_SESSION_ONLY), USER_ID);

            verify(sessionRepository).revokeById(SESSION_ID);
            verify(sessionCachePort).evictSession(SESSION_ID, USER_ID);
        }

        @Test
        @DisplayName("DB revoked before cache evicted — correct order to avoid stale cache on rollback")
        void dbBeforeCache() {
            InOrder order = inOrder(sessionRepository, sessionCachePort);

            handler.handle(command(LogoutScope.CURRENT_SESSION_ONLY), USER_ID);

            order.verify(sessionRepository).revokeById(SESSION_ID);
            order.verify(sessionCachePort).evictSession(SESSION_ID, USER_ID);
        }

        @Test
        @DisplayName("should NOT touch other sessions — no allSessions methods")
        void doesNotAffectOtherSessions() {
            handler.handle(command(LogoutScope.CURRENT_SESSION_ONLY), USER_ID);

            verify(sessionRepository, never()).revokeAllByUserId(any());
            verify(sessionRepository, never()).revokeAllByUserIdExceptSessionId(any(), any());
            verify(sessionCachePort, never()).evictAllSessions(any());
        }

        @Test
        @DisplayName("should be idempotent — calling twice does not throw")
        void idempotent() {
            assertThatCode(() -> {
                handler.handle(command(LogoutScope.CURRENT_SESSION_ONLY), USER_ID);
                handler.handle(command(LogoutScope.CURRENT_SESSION_ONLY), USER_ID);
            }).doesNotThrowAnyException();

            verify(sessionRepository, times(2)).revokeById(SESSION_ID);
            verify(sessionCachePort, times(2)).evictSession(SESSION_ID, USER_ID);
        }

        @Test
        @DisplayName("should revoke exactly SESSION_ID — not another session")
        void scopedToSessionId() {
            SessionId otherSession = SessionId.of(999L);

            handler.handle(command(LogoutScope.CURRENT_SESSION_ONLY), USER_ID);

            verify(sessionRepository).revokeById(SESSION_ID);
            verify(sessionRepository, never()).revokeById(otherSession);
            verify(sessionCachePort).evictSession(SESSION_ID, USER_ID);
            verify(sessionCachePort, never()).evictSession(eq(otherSession), any());
        }

        @Test
        @DisplayName("exactly two operations — revokeById + evictSession, nothing more")
        void exactlyTwoOperations() {
            handler.handle(command(LogoutScope.CURRENT_SESSION_ONLY), USER_ID);

            verify(sessionRepository).revokeById(SESSION_ID);
            verify(sessionCachePort).evictSession(SESSION_ID, USER_ID);
            verifyNoMoreInteractions(sessionRepository, sessionCachePort);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  2. ALL_EXCEPT_CURRENT
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("ALL_EXCEPT_CURRENT")
    class AllExceptCurrent {

        @Test
        @DisplayName("happy path — revoke others in DB, evict all cache, re-cache current")
        void happyPath_sessionExists() {
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(fakeSession()));

            handler.handle(command(LogoutScope.ALL_EXCEPT_CURRENT), USER_ID);

            verify(sessionRepository).revokeAllByUserIdExceptSessionId(USER_ID, SESSION_ID);
            verify(sessionCachePort).evictAllSessions(USER_ID);
            verify(sessionCachePort).cacheSession(SESSION_ID, USER_ID, EXPIRES_AT);
        }

        @Test
        @DisplayName("should ALWAYS evict all from cache even when current session missing in DB")
        void evictsAllEvenIfCurrentSessionMissing() {
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

            handler.handle(command(LogoutScope.ALL_EXCEPT_CURRENT), USER_ID);

            verify(sessionCachePort).evictAllSessions(USER_ID);
            // cannot re-cache without expiresAt data
            verify(sessionCachePort, never()).cacheSession(any(), any(), any());
        }

        @Test
        @DisplayName("findById query before evictAll — minimizes window where current session is absent from cache")
        void findByIdBeforeEvict() {
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(fakeSession()));

            InOrder dbOrder    = inOrder(sessionRepository);
            InOrder cacheOrder = inOrder(sessionCachePort);

            handler.handle(command(LogoutScope.ALL_EXCEPT_CURRENT), USER_ID);

            dbOrder.verify(sessionRepository).revokeAllByUserIdExceptSessionId(USER_ID, SESSION_ID);
            dbOrder.verify(sessionRepository).findById(SESSION_ID);
            cacheOrder.verify(sessionCachePort).evictAllSessions(USER_ID);
            cacheOrder.verify(sessionCachePort).cacheSession(any(), any(), any());
        }

        @Test
        @DisplayName("should NOT revoke the current session in DB")
        void doesNotRevokeCurrentSession() {
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(fakeSession()));

            handler.handle(command(LogoutScope.ALL_EXCEPT_CURRENT), USER_ID);

            verify(sessionRepository, never()).revokeById(any());
            verify(sessionRepository, never()).revokeAllByUserId(any());
        }

        @Test
        @DisplayName("should scope revoke to correct userId only")
        void scopedToCorrectUserId() {
            UserId other = UserId.of(999L);
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(fakeSession()));

            handler.handle(command(LogoutScope.ALL_EXCEPT_CURRENT), USER_ID);

            verify(sessionRepository).revokeAllByUserIdExceptSessionId(eq(USER_ID), eq(SESSION_ID));
            verify(sessionRepository, never()).revokeAllByUserIdExceptSessionId(eq(other), any());
            verify(sessionCachePort).evictAllSessions(USER_ID);
            verify(sessionCachePort, never()).evictAllSessions(other);
        }

        @Test
        @DisplayName("re-cached session uses expiresAt from DB — not from AT claim")
        void reusesExpiresAtFromDb() {
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(fakeSession()));

            handler.handle(command(LogoutScope.ALL_EXCEPT_CURRENT), USER_ID);

            // EXPIRES_AT matches fakeSession().getExpiresAt()
            verify(sessionCachePort).cacheSession(SESSION_ID, USER_ID, EXPIRES_AT);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  3. ALL_SESSIONS
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("ALL_SESSIONS")
    class AllSessions {

        @Test
        @DisplayName("should revoke all in DB and evict all from cache")
        void happyPath() {
            handler.handle(command(LogoutScope.ALL_SESSIONS), USER_ID);

            verify(sessionRepository).revokeAllByUserId(USER_ID);
            verify(sessionCachePort).evictAllSessions(USER_ID);
        }

        @Test
        @DisplayName("DB revoked before cache evicted — correct order")
        void dbBeforeCache() {
            InOrder order = inOrder(sessionRepository, sessionCachePort);

            handler.handle(command(LogoutScope.ALL_SESSIONS), USER_ID);

            order.verify(sessionRepository).revokeAllByUserId(USER_ID);
            order.verify(sessionCachePort).evictAllSessions(USER_ID);
        }

        @Test
        @DisplayName("should NOT call per-session methods")
        void doesNotUseIndividualMethods() {
            handler.handle(command(LogoutScope.ALL_SESSIONS), USER_ID);

            verify(sessionRepository, never()).revokeById(any());
            verify(sessionCachePort, never()).evictSession(any(), any());
            verify(sessionCachePort, never()).cacheSession(any(), any(), any());
            verify(sessionRepository, never()).findById(any());
        }

        @Test
        @DisplayName("should scope to correct userId — cannot revoke another user's sessions")
        void scopedToCorrectUser() {
            UserId victim = UserId.of(7L);

            handler.handle(command(LogoutScope.ALL_SESSIONS), USER_ID);

            verify(sessionRepository).revokeAllByUserId(USER_ID);
            verify(sessionRepository, never()).revokeAllByUserId(victim);
            verify(sessionCachePort).evictAllSessions(USER_ID);
            verify(sessionCachePort, never()).evictAllSessions(victim);
        }

        @Test
        @DisplayName("calling twice should not throw — idempotent")
        void idempotent() {
            assertThatCode(() -> {
                handler.handle(command(LogoutScope.ALL_SESSIONS), USER_ID);
                handler.handle(command(LogoutScope.ALL_SESSIONS), USER_ID);
            }).doesNotThrowAnyException();

            verify(sessionRepository, times(2)).revokeAllByUserId(USER_ID);
            verify(sessionCachePort, times(2)).evictAllSessions(USER_ID);
        }

        @Test
        @DisplayName("exactly two operations — revokeAllByUserId + evictAllSessions, nothing more")
        void exactlyTwoOperations() {
            handler.handle(command(LogoutScope.ALL_SESSIONS), USER_ID);

            verify(sessionRepository).revokeAllByUserId(USER_ID);
            verify(sessionCachePort).evictAllSessions(USER_ID);
            verifyNoMoreInteractions(sessionRepository, sessionCachePort);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  4. Scope isolation — no cross-contamination between scopes
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Scope isolation")
    class ScopeIsolation {

        @Test
        @DisplayName("CURRENT_SESSION_ONLY never calls revokeAllByUserId")
        void currentOnly_neverRevokesAll() {
            handler.handle(command(LogoutScope.CURRENT_SESSION_ONLY), USER_ID);
            verify(sessionRepository, never()).revokeAllByUserId(any());
        }

        @Test
        @DisplayName("ALL_SESSIONS never calls revokeById")
        void allSessions_neverRevokesById() {
            handler.handle(command(LogoutScope.ALL_SESSIONS), USER_ID);
            verify(sessionRepository, never()).revokeById(any());
        }

        @Test
        @DisplayName("ALL_SESSIONS never calls findById — no unnecessary DB query")
        void allSessions_neverCallsFindById() {
            handler.handle(command(LogoutScope.ALL_SESSIONS), USER_ID);
            verify(sessionRepository, never()).findById(any());
        }

        @Test
        @DisplayName("CURRENT_SESSION_ONLY never calls evictAllSessions")
        void currentOnly_neverEvictsAll() {
            handler.handle(command(LogoutScope.CURRENT_SESSION_ONLY), USER_ID);
            verify(sessionCachePort, never()).evictAllSessions(any());
        }
    }
}
