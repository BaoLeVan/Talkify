package com.talkify.identity.domain.model;

import java.util.Objects;

public record SessionId(Long value) {

    public SessionId {
        Objects.requireNonNull(value, "SessionId cannot be null");
        if (value <= 0) {
            throw new IllegalArgumentException("SessionId must be a positive integer");
        }
    }

    public static SessionId of(Long value) {
        return new SessionId(value);
    }
}
