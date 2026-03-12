package com.talkify.identity.application.port;

import com.talkify.identity.domain.model.UserId;

import io.jsonwebtoken.Claims;

public interface JwtPort {
    String generateAccessToken(UserId userId, String email, String role);
    String generateRefreshToken(UserId userId);
    boolean validateToken(String token);
    UserId extractUserId(String token);
    public Claims extractAllClaims(String token);
}
