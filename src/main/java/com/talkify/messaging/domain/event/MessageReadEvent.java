package com.talkify.messaging.domain.event;

import java.time.Instant;

import com.talkify.common.domain.DomainEvent;
import com.talkify.common.domain.UserId;
import com.talkify.messaging.domain.model.ConversationId;
import com.talkify.messaging.domain.model.MessageId;

/**
 * Emitted when a message is marked as read.
 * Subscribers: update delivery receipt, WebSocket push read receipt to sender.
 */
public record MessageReadEvent(
        MessageId      messageId,
        ConversationId conversationId,
        UserId         readBy,
        long           lastReadSequence,
        Instant        readAt
) implements DomainEvent {
    @Override
    public Instant occurredAt() { return readAt; }
}
