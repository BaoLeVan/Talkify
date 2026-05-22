package com.talkify.messaging.interfaces.rest.request;

import java.util.List;

import com.talkify.messaging.domain.model.Attachment;
import com.talkify.messaging.domain.model.MessageId;
import com.talkify.messaging.domain.model.MessageType;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/**
 * Interface-layer DTO — owns format and structural validation.
 * Mapped to SendMessageCommand before reaching the application layer.
 */
public record SendMessageRequest(
    Long conversationId,
    Long recipientId,
    @NotNull MessageType messageType,
    String text,
    List<Attachment> attachments,
    MessageId replyToMessageId
) {
    @AssertTrue(message = "Either conversationId or recipientId must be provided")
    public boolean isTargetPresent() {
        return conversationId != null || recipientId != null;
    }
}
