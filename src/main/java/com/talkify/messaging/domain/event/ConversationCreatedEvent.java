package com.talkify.messaging.domain.event;

import java.time.Instant;

import com.talkify.messaging.domain.model.ConversationId;
import com.talkify.messaging.domain.model.ConversationType;

/**
 * Emitted when a new conversation (DIRECT or GROUP) is created.
 * Subscribers: analytics, notification to added participants.
 */
public record ConversationCreatedEvent(
        ConversationId   conversationId,
        ConversationType type,
        Instant          occurredAt
) implements DomainEvent {
    public ConversationCreatedEvent(ConversationId conversationId, ConversationType type) {
        this(conversationId, type, Instant.now());
    }
}
