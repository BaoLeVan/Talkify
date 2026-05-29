package com.talkify.messaging.domain.event;

import java.time.Instant;
import java.util.List;

import com.talkify.messaging.domain.model.ConversationType;
import com.talkify.messaging.domain.model.MessageType;

/**
 * Lean event — chỉ chứa data tối thiểu để FE:
 *   1. Route đúng conversation
 *   2. Render tin nhắn (content)
 *   3. Resolve sender từ local profile cache
 * 
 * Sender profile KHÔNG gửi kèm vì:
 *   - FE đã cache profile khi load conversation list
 *   - Giảm payload ~60% (không gửi name, avatar mỗi message)
 *   - Tránh stale data (user đổi tên → event cũ vẫn hiển thị tên cũ)
 */
public record MessageSentEvent(
    String conversationId,
    ConversationType conversationType,       // "DIRECT" | "GROUP"
    List<Long> recipientIds,

    String messageId,
    long sequenceNumber,
    long senderId,
    MessageType messageType,
    String text,
    List<AttachmentPayload> attachments,
    ReplyPayload replyTo,
    Instant sentAt
) {
    public record AttachmentPayload(
        String type, String url, String name, long size
    ) {}

    public record ReplyPayload(
        String messageId, String previewText, String type
    ) {}
}