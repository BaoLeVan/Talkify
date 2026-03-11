package com.talkify.identity.domain.model;

import java.util.regex.Pattern;

public record Password(String value) {
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$");

    public static Password ofHashed(String hashed) {
        return new Password(hashed);
    }

    public static void validateRaw(String raw) {
        if (raw == null || !PASSWORD_PATTERN.matcher(raw).matches()) {
            throw new IllegalArgumentException("Password must be at least 8 characters long and contain both letters and numbers");
        }
    }
}