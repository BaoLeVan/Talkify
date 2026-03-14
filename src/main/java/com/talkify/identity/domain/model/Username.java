package com.talkify.identity.domain.model;

public record Username(String value) {
    public Username {
        if (value == null || value.trim().isEmpty() ) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
    }

    public static Username of(String value) {
        return new Username(value);
    }

    public static boolean isValid(String value) {
        return value != null;
    }
}