package com.talkify.identity.domain.repository;

import com.talkify.identity.domain.model.SessionId;
import com.talkify.identity.domain.model.UserId;
import com.talkify.identity.domain.model.UserSession;

import java.util.List;
import java.util.Optional;

public interface SessionRepository {

    UserSession save(UserSession session);
    Optional<UserSession> findByTokenHash(String tokenHash);
    List<UserSession> findAllActiveByUserId(UserId userId);
    void revokeByTokenHash(String tokenHash);
    void revokeByIds(UserId userId, List<SessionId> sessionIds);
    void revokeAllByUserId(UserId userId);
    void deleteExpired();
}
