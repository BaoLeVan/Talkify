package com.talkify.identity.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.talkify.config.security.JwtProperties;
import com.talkify.identity.application.port.JwtPort;
import com.talkify.identity.application.port.TokenClaims;
import com.talkify.identity.domain.model.UserId;
import com.talkify.identity.domain.model.UserRole;
import com.talkify.identity.domain.model.UserStatus;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAdapter implements JwtPort{
    private final JwtProperties jwtProperties;
    private SecretKey signingKey;

    private SecretKey getSigningKey() {
        if (signingKey == null) {
            signingKey = Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8));
        }
        return signingKey;
    }

    @Override
    public String generateAccessToken(UserId userId, UserRole role, UserStatus status) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId.value()))
                .claim("role", role.name())
                .claim("type", "access")
                .claim("status", status.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(jwtProperties.getAccessTokenTtl())))
                .signWith(getSigningKey())
                .compact();
    }

    @Override
    public String generateRefreshToken(UserId userId) {
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
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public UserId extractUserId(String token) {
        String subject = parseClaims(token).getPayload().getSubject();
        return UserId.of(Long.valueOf(subject));
    }

    private Jws<Claims> parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
    }

    @Override
    public TokenClaims extractAllClaims(String token) {
        Claims payload = parseClaims(token).getPayload();
        return new TokenClaims(
                payload.getSubject(),
                payload.get("type", String.class),
                payload.get("role", String.class),
                payload.get("status", String.class)
        );
    }
}
