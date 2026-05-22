package com.talkify.messaging.domain.event;

import java.time.Instant;

import com.talkify.common.domain.UserId;
import com.talkify.messaging.domain.model.ConversationId;
import com.talkify.messaging.domain.model.MessageContent;
import com.talkify.messaging.domain.model.MessageId;
import com.talkify.messaging.domain.model.MessageType;

/**
 * Emitted when a new message is successfully created in the domain.
 * Subscribers: real-time delivery (WebSocket), notification service, analytics.
 */
public record MessageSentEvent(
        MessageId      messageId,
        ConversationId conversationId,
        UserId         senderId,
        MessageContent content,
        MessageType    type,
        long           sequenceNumber,
        Instant        occurredAt
) implements DomainEvent {}
