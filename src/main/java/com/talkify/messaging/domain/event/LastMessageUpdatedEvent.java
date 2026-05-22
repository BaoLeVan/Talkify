package com.talkify.messaging.domain.event;

import java.time.Instant;

import com.talkify.messaging.domain.model.ConversationId;
import com.talkify.messaging.domain.model.MessageId;

public record LastMessageUpdatedEvent(
    ConversationId conversationId,
    MessageId messageId,
    String messagePreview,
    Instant lastMessageAt
) implements DomainEvent {
    @Override
    public Instant occurredAt() { return lastMessageAt; }
}
