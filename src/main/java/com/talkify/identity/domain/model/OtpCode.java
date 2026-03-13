package com.talkify.identity.domain.model;

import java.time.Instant;
import java.util.Objects;

public record OtpCode(String code, Instant expiresAt, boolean isUsed) {
    private static final int OTP_LENGTH = 6;
    private static final int OTP_TTL_SECONDS = 300; // 5 minutes

    public OtpCode {
        Objects.requireNonNull(code, "OTP code cannot be null");
        if (code.length() != OTP_LENGTH) {
            throw new IllegalArgumentException("OTP code must be " + OTP_LENGTH + " digits");
        }
        Objects.requireNonNull(expiresAt, "Expiration time cannot be null");
    }

    public static OtpCode generate(String code) {
        Instant expiresAt = Instant.now().plusSeconds(OTP_TTL_SECONDS);
        return new OtpCode(code, expiresAt, false);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public OtpCode  markAsUsed() {
        return new OtpCode(code, expiresAt, true);
    }

    public boolean isValid() {
        return !isUsed() && !isExpired();
    }

}
