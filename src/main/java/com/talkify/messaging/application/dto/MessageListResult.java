package com.talkify.messaging.application.dto;

import java.time.Instant;
import java.util.List;

/**
 * Application-layer projection for paginated message list.
 * Decoupled from REST response format.
 */
public record MessageListResult(
    String conversationId,
    List<MessageProjection> items,
    String nextCursor,
    String prevCursor,
    int size
) {

    public record MessageProjection(
        long id,
        long sequenceNumber,
        String type,
        SenderInfo sender,
        ContentInfo content,
        ReplyInfo replyTo,
        Instant sentAt,
        Instant editedAt,
        boolean revoked
    ) {}

    public record SenderInfo(
        long userId,
        String displayName,
        String avatarUrl
    ) {}

    public record ContentInfo(
        String text,
        List<AttachmentInfo> attachments
    ) {}

    public record AttachmentInfo(
        String fileId,
        String fileName,
        String mimeType,
        long fileSize,
        String url,
        String thumbnailUrl
    ) {}

    public record ReplyInfo(
        long messageId,
        long senderId,
        String senderName,
        String type,
        String previewText
    ) {}
}
