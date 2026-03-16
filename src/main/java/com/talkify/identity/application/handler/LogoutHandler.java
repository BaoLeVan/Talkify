package com.talkify.identity.application.handler;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.talkify.common.util.Sha256Utils;
import com.talkify.identity.application.command.LogoutCommand;
import com.talkify.identity.domain.model.UserId;
import com.talkify.identity.domain.repository.SessionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Use Case: thu hồi session theo 3 scope:
 *
 *  CURRENT_SESSION_ONLY  — revoke đúng session đang dùng (cookie RT).
 *                           Không có cookie → idempotent (session đã bị hủy trước).
 *  ALL_EXCEPT_CURRENT    — revoke tất cả session khác — "kick các thiết bị khác".
 *                           Không có cookie → degrade xuống ALL_SESSIONS.
 *  ALL_SESSIONS          — revoke toàn bộ — không cần RT, chỉ cần userId.
 *
 * Idempotent: luôn trả về thành công, không throw exception.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogoutHandler {

    private final SessionRepository sessionRepository;

    @Transactional
    public void handle(LogoutCommand command, UserId userId) {
        String rawToken  = command.rawRefreshToken();
        boolean hasToken = rawToken != null && !rawToken.isBlank();

        switch (command.scope()) {
            case CURRENT_SESSION_ONLY -> {
                if (!hasToken) {
                    log.debug("Logout CURRENT_ONLY: no RT cookie, nothing to revoke (idempotent)");
                } else {
                    sessionRepository.revokeByTokenHash(Sha256Utils.hash(rawToken));
                    log.info("Session revoked (current) | userId={}", userId.value());
                }
            }
            case ALL_EXCEPT_CURRENT -> {
                if (!hasToken) {
                    log.info("Logout ALL_EXCEPT_CURRENT: no RT cookie, degrading to ALL_SESSIONS | userId={}", userId.value());
                    sessionRepository.revokeAllByUserId(userId);
                } else {
                    sessionRepository.revokeAllByUserIdExceptTokenHash(userId, Sha256Utils.hash(rawToken));
                    log.info("All other sessions revoked | userId={}", userId.value());
                }
            }
            case ALL_SESSIONS -> {
                sessionRepository.revokeAllByUserId(userId);
                log.info("All sessions revoked | userId={}", userId.value());
            }
        }
    }
}
