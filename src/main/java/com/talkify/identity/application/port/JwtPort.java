package com.talkify.identity.application.port;

import com.talkify.identity.domain.model.UserId;

public interface JwtPort {
    String generateAccessToken(UserId userId, String email, String role);
    String generateRefreshToken(UserId userId);
    boolean validateToken(String token);
    UserId extractUserId(String token);
}
