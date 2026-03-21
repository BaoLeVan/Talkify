package com.talkify.identity.application.port;

import com.talkify.identity.domain.model.SessionId;
import com.talkify.identity.domain.model.UserId;
import com.talkify.identity.domain.model.UserRole;
import com.talkify.identity.domain.model.UserStatus;

public interface JwtPort {

    String issueAccessToken(UserId userId, SessionId sessionId, UserRole role, UserStatus status);
    String issueRefreshToken(UserId userId);
    TokenParseResult parseAccessToken(String token);
    boolean validateRefreshToken(String token);
    long refreshTokenTtl();
    long refreshThreshold();
}
