package com.talkify.messaging.domain.event;

import java.time.Instant;

import com.talkify.common.domain.UserId;
import com.talkify.messaging.domain.model.ConversationId;

public record ParticipantRemovedEvent(
    ConversationId conversationId,
    UserId removedUserId,
    UserId removedBy,
    Instant removedAt
) implements DomainEvent {
    @Override
    public Instant occurredAt() { return removedAt; }
}
