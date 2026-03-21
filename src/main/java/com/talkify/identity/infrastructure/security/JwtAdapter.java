package com.talkify.identity.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.talkify.config.security.JwtProperties;
import com.talkify.identity.application.port.JwtPort;
import com.talkify.identity.application.port.TokenClaims;
import com.talkify.identity.application.port.TokenParseResult;
import com.talkify.identity.domain.model.SessionId;
import com.talkify.identity.domain.model.UserId;
import com.talkify.identity.domain.model.UserRole;
import com.talkify.identity.domain.model.UserStatus;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAdapter implements JwtPort{
    private final JwtProperties jwtProperties;
    private SecretKey signingKey;

    @PostConstruct
    void init() {
        byte[] keyBytes = jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                "JWT secret key must be at least 32 bytes, got " + keyBytes.length);
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    private SecretKey getSigningKey() {
        return signingKey;
    }

    @Override
    public String issueAccessToken(UserId userId, SessionId sessionId, UserRole role, UserStatus status) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId.value()))
                .claim("sid", sessionId.value())
                .claim("role", role.name())
                .claim("type", "access")
                .claim("status", status.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(jwtProperties.getAccessTokenTtl())))
                .signWith(getSigningKey())
                .compact();
    }

    @Override
    public String issueRefreshToken(UserId userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId.value()))
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(jwtProperties.getRefreshTokenTtl())))
                .signWith(getSigningKey())
                .compact();
    }

    @Override
    public TokenParseResult parseAccessToken(String token) {
        try {
            Claims payload = parseClaims(token).getPayload();
            TokenClaims claims = new TokenClaims(
                    payload.getSubject(),
                    payload.get("type", String.class),
                    payload.get("role", String.class),
                    payload.get("status", String.class),
                    SessionId.of(payload.get("sid", Long.class))
            );
            return new TokenParseResult.Valid(claims);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return new TokenParseResult.Expired();
        } catch (Exception e) {
            log.warn("Invalid access token: {}", e.getMessage());
            return new TokenParseResult.Invalid();
        }
    }

    @Override
    public boolean validateRefreshToken(String token) {
        try {
            Claims payload = parseClaims(token).getPayload();
            return "refresh".equals(payload.get("type", String.class));
        } catch (Exception e) {
            log.warn("Invalid refresh token: {}", e.getMessage());
            return false;
        }
    }

    private Jws<Claims> parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
    }

    @Override
    public long refreshTokenTtl() {
        return jwtProperties.getRefreshTokenTtl();
    }

    @Override
    public long refreshThreshold() {
        return jwtProperties.getRefreshThreshold();
    }
}
