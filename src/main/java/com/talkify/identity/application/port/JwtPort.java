package com.talkify.identity.application.port;

import com.talkify.identity.domain.model.UserId;
import com.talkify.identity.domain.model.UserStatus;

public interface JwtPort {
    String generateAccessToken(UserId userId, String email, String role, UserStatus status);
    String generateRefreshToken(UserId userId);
    boolean validateToken(String token);
    UserId extractUserId(String token);
    TokenClaims extractAllClaims(String token);
}
