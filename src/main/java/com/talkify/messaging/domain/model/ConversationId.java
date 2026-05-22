package com.talkify.messaging.domain.model;

public record ConversationId(Long value) {
    public ConversationId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("ConversationId must be a positive integer");
        }
    }

    public static ConversationId of(Long value) {
        return new ConversationId(value);
    }
}