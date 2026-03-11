package com.talkify.identity.domain.model;

import java.util.Objects;

public record UserId(Long value) {
    public UserId {
        Objects.requireNonNull(value, "UserId cannot be null");
        if (value <= 0) {
            throw new IllegalArgumentException("UserId must be a positive integer");
        }
    }

    public static UserId of(Long value) {
        return new UserId(value);
    }
}