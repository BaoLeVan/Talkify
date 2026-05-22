package com.talkify.messaging.domain.event;

import java.time.Instant;

import com.talkify.common.domain.UserId;
import com.talkify.messaging.domain.model.ConversationId;

public record ParticipantAddedEvent(
    ConversationId conversationId,
    UserId addedUserId,
    Instant addedAt
) implements DomainEvent {
    @Override
    public Instant occurredAt() { return addedAt; }
}
