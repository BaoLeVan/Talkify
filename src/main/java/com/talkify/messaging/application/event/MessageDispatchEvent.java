package com.talkify.messaging.application.event;

import java.util.List;

public record MessageDispatchEvent(
    String conversationId,
    String conversationType,
    List<Long> recipientIds,
    String messageId,
    long sequenceNumber,
    long senderId,
    String messageType,
    String text,
    List<AttachmentPayload> attachments,
    ReplyPayload replyTo,
    String sentAt
) {
    public record AttachmentPayload(
        String type, String url, String name, long size
    ) {}

    public record ReplyPayload(
        String messageId, String previewText, String type
    ) {}
}
