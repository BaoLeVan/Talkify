package com.talkify.messaging.domain.event;

import java.time.Instant;

import com.talkify.messaging.domain.model.ConversationId;

public record ConversationChangedTitleEvent(
    ConversationId conversationId,
    String newTitle,
    Instant occurredAt
) implements DomainEvent {
    public ConversationChangedTitleEvent(ConversationId conversationId, String newTitle) {
        this(conversationId, newTitle, Instant.now());
    }
}
