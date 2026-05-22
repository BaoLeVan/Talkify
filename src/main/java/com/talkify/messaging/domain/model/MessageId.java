package com.talkify.messaging.domain.model;

public record MessageId(Long value) {
    public MessageId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("MessageId must be a positive integer");
        }
    }

    public static MessageId of(Long value) {
        return new MessageId(value);
    }
}
