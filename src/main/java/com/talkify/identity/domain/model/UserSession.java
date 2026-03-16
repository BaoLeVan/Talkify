package com.talkify.identity.domain.model;

import java.time.Instant;
import java.util.Objects;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class UserSession {

    private SessionId id;
    private final UserId userId;
    private final String tokenHash;
    private final DeviceInfo deviceInfo;
    private final Instant expiresAt;
    private Instant lastUsedAt;
    @Builder.Default
    private final Instant createdAt = Instant.now();
    private Instant revokedAt;

    public boolean isExpired()             { return isExpired(Instant.now()); }
    public boolean isExpired(Instant now)  { return now.isAfter(expiresAt); }
    public boolean isRevoked()             { return revokedAt != null; }
    public boolean isValid()               { return !isRevoked() && !isExpired(); }
    public boolean isValid(Instant now)    { return !isRevoked() && !isExpired(now); }

    public void revoke() {
        revoke(Instant.now());
    }

    public void revoke(Instant now) {
        if (!isRevoked()) {
            this.revokedAt = now;
        }
    }

    public void markUsed() {
        markUsed(Instant.now());
    }

    public void markUsed(Instant now) {
        this.lastUsedAt = now;
    }

    public void assignId(SessionId id) {
        Objects.requireNonNull(id, "SessionId cannot be null");
        this.id = id;
    }

    // ── Static factories ─────────────────────────────────────────────────────
    public static UserSession create(
            UserId userId,
            String tokenHash,
            DeviceInfo deviceInfo,
            Instant expiresAt,
            Instant createdAt
    ) {
        Objects.requireNonNull(userId,     "userId cannot be null");
        Objects.requireNonNull(tokenHash,  "tokenHash cannot be null");
        Objects.requireNonNull(deviceInfo, "deviceInfo cannot be null");
        Objects.requireNonNull(expiresAt,  "expiresAt cannot be null");
        Objects.requireNonNull(createdAt,  "createdAt cannot be null");
        return UserSession.builder()
                .userId(userId)
                .tokenHash(tokenHash)
                .deviceInfo(deviceInfo)
                .expiresAt(expiresAt)
                .createdAt(createdAt)
                .build();
    }

    public static UserSession reconstruct(
            SessionId id,
            UserId userId,
            String tokenHash,
            DeviceInfo deviceInfo,
            Instant expiresAt,
            Instant lastUsedAt,
            Instant createdAt,
            Instant revokedAt
    ) {
        return UserSession.builder()
                .id(id)
                .userId(userId)
                .tokenHash(tokenHash)
                .deviceInfo(deviceInfo)
                .expiresAt(expiresAt)
                .lastUsedAt(lastUsedAt)
                .createdAt(createdAt)
                .revokedAt(revokedAt)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserSession other)) return false;
        if (id == null && other.id == null) return Objects.equals(tokenHash, other.tokenHash);
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hash(id) : Objects.hash(tokenHash);
    }
}

