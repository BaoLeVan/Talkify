package com.talkify.messaging.domain.event;

import java.time.Instant;

import com.talkify.common.domain.DomainEvent;
import com.talkify.common.domain.UserId;
import com.talkify.messaging.domain.model.ConversationId;
import com.talkify.messaging.domain.model.MessageId;

/**
 * Emitted when a message text is edited by its sender.
 * Subscribers: WebSocket push MESSAGE_EDITED to all participants.
 */
public record MessageEditedEvent(
        MessageId      messageId,
        ConversationId conversationId,
        UserId         editedBy,
        String         newText,
        Instant        editedAt
) implements DomainEvent {
    @Override
    public Instant occurredAt() { return editedAt; }
}
