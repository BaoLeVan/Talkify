package com.talkify.messaging.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.talkify.common.domain.AggregateRoot;
import com.talkify.common.domain.UserId;
import com.talkify.common.exception.AppException;
import com.talkify.common.exception.ErrorCode;
import com.talkify.common.id.IdGenerator;
import com.talkify.messaging.domain.event.MessageEditedEvent;
import com.talkify.messaging.domain.event.MessageReadEvent;
import com.talkify.messaging.domain.event.MessageRevokedEvent;
import com.talkify.messaging.domain.event.MessageSentEvent;

import lombok.Getter;

@Getter
public class Message extends AggregateRoot {

    private static final Duration REVOKE_WINDOW = Duration.ofMinutes(15);
    private static final Duration EDIT_WINDOW   = Duration.ofMinutes(15);

    private MessageId       id;
    private ConversationId  conversationId;
    private UserId          senderId;
    private MessageType     type;
    private MessageContent  content;
    private long            sequenceNumber;
    private MessageId       replyToMessageId;   // nullable

    private boolean         revoked;
    private Instant         revokedAt;

    private Instant         editedAt;

    private Instant         createdAt;

    // ── Factory ──────────────────────────────────────────────────────────────

    /**
     * Tạo tin nhắn mới.
     *
     * @param idGenerator  dùng để sinh Snowflake ID (truyền vào để domain
     *                     không phụ thuộc Spring bean)
     */
    public static Message create(IdGenerator idGenerator,
                                 ConversationId conversationId,
                                 UserId senderId,
                                 MessageType type,
                                 MessageContent content,
                                 long sequenceNumber,
                                 MessageId replyToMessageId) {
        Message msg = new Message();
        msg.id              = MessageId.of(idGenerator.nextId());
        msg.conversationId  = conversationId;
        msg.senderId        = senderId;
        msg.type            = type;
        msg.content         = content;
        msg.sequenceNumber  = sequenceNumber;
        msg.replyToMessageId = replyToMessageId;
        msg.revoked         = false;
        msg.createdAt       = Instant.now();

        return msg;
    }

    // ── Restore (infrastructure only — no events, no validation) ─────────────

    public static Message restore(MessageId id,
                                  ConversationId conversationId,
                                  UserId senderId,
                                  MessageType type,
                                  MessageContent content,
                                  long sequenceNumber,
                                  MessageId replyToMessageId,
                                  boolean revoked,
                                  Instant revokedAt,
                                  Instant editedAt,
                                  Instant createdAt) {
        Message msg = new Message();
        msg.id               = id;
        msg.conversationId   = conversationId;
        msg.senderId         = senderId;
        msg.type             = type;
        msg.content          = content;
        msg.sequenceNumber   = sequenceNumber;
        msg.replyToMessageId = replyToMessageId;
        msg.revoked          = revoked;
        msg.revokedAt        = revokedAt;
        msg.editedAt         = editedAt;
        msg.createdAt        = createdAt;
        return msg;
    }

    // ── Behavior ─────────────────────────────────────────────────────────────

    /**
     * Thu hồi tin nhắn (soft-delete hiển thị "Tin nhắn đã bị thu hồi").
     * Chỉ người gửi được thu hồi, trong vòng 15 phút.
     */
    public void revoke(UserId actor) {
        if (!senderId.equals(actor))
            throw new AppException(ErrorCode.CANNOT_REVOKE_MESSAGE);
        if (revoked)
            throw new AppException(ErrorCode.MESSAGE_ALREADY_REVOKED);
        if (Duration.between(createdAt, Instant.now()).compareTo(REVOKE_WINDOW) > 0)
            throw new AppException(ErrorCode.REVOKE_WINDOW_EXPIRED);

        this.revoked   = true;
        this.revokedAt = Instant.now();
        // Xóa nội dung — client hiển thị "Tin nhắn đã bị thu hồi"
        this.content   = null;

        registerEvent(new MessageRevokedEvent(id, conversationId, actor, revokedAt));
    }

    /**
     * Chỉnh sửa nội dung tin nhắn TEXT.
     * Chỉ người gửi, chỉ type TEXT, trong vòng 15 phút.
     */
    public void edit(UserId actor, String newText) {
        if (!senderId.equals(actor))
            throw new AppException(ErrorCode.MESSAGE_EDIT_FORBIDDEN);
        if (revoked)
            throw new AppException(ErrorCode.MESSAGE_ALREADY_REVOKED);
        if (type != MessageType.TEXT)
            throw new AppException(ErrorCode.MESSAGE_EDIT_FORBIDDEN);
        if (Duration.between(createdAt, Instant.now()).compareTo(EDIT_WINDOW) > 0)
            throw new AppException(ErrorCode.MESSAGE_EDIT_EXPIRED);

        this.content  = new MessageContent(newText, List.of(), content.metadata());
        this.editedAt = Instant.now();

        registerEvent(new MessageEditedEvent(id, conversationId, actor, newText, editedAt));
    }

    /**
     * Đánh dấu đã đọc — emit event để cập nhật delivery receipt.
     */
    public void markRead(UserId reader, Instant readAt) {
        registerEvent(new MessageReadEvent(id, conversationId, reader, sequenceNumber, readAt));
    }

    // ── Convenience ──────────────────────────────────────────────────────────

    public boolean isRevoked()  { return revoked; }
    public boolean isEdited()   { return editedAt != null; }
    public boolean isReply()    { return replyToMessageId != null; }
}
