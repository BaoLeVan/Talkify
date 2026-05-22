package com.talkify.messaging.interfaces.rest.response;

import java.time.Instant;

import com.talkify.messaging.domain.model.MessageType;

/**
 * REST response DTO for a single message.
 * No domain model imports (only enum for serialization).
 * Construction handled by MessageAssembler.
 */
public record MessageResponse(
    long id,
    long sequenceNumber,
    MessageType type,
    SenderResponse sender,
    MessageContentResponse content,
    ReplyToResponse replyTo,
    Instant sentAt,
    Instant editedAt
) {}