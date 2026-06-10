package com.talkify.messaging.domain.event;

import java.time.Instant;

import com.talkify.common.domain.DomainEvent;
import com.talkify.common.domain.UserId;
import com.talkify.messaging.domain.model.ConversationId;
import com.talkify.messaging.domain.model.MessageId;

/**
 * Emitted when a message is revoked by its sender.
 * Subscribers: WebSocket push MESSAGE_REVOKED to all participants.
 */
public record MessageRevokedEvent(
        MessageId      messageId,
        ConversationId conversationId,
        UserId         revokedBy,
        Instant        revokedAt
) implements DomainEvent {
    @Override
    public Instant occurredAt() { return revokedAt; }
}
