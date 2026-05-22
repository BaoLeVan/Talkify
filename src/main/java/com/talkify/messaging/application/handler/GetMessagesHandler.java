package com.talkify.messaging.application.handler;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.talkify.common.domain.UserInfo;
import com.talkify.common.exception.AppException;
import com.talkify.common.exception.ErrorCode;
import com.talkify.messaging.application.command.GetMessagesCommand;
import com.talkify.messaging.application.dto.MessageListResult;
import com.talkify.messaging.application.dto.MessageListResult.AttachmentInfo;
import com.talkify.messaging.application.dto.MessageListResult.ContentInfo;
import com.talkify.messaging.application.dto.MessageListResult.MessageProjection;
import com.talkify.messaging.application.dto.MessageListResult.ReplyInfo;
import com.talkify.messaging.application.dto.MessageListResult.SenderInfo;
import com.talkify.messaging.application.port.UserQueryPort;
import com.talkify.messaging.domain.model.Conversation;
import com.talkify.messaging.domain.model.CursorDirection;
import com.talkify.messaging.domain.model.Message;
import com.talkify.messaging.domain.model.MessageCursor;
import com.talkify.messaging.domain.repository.ConversationRepository;
import com.talkify.messaging.domain.repository.MessageRepository;

import lombok.RequiredArgsConstructor;

/**
 * QUERY USE CASE: Get paginated messages for a conversation.
 * 
 * Responsibilities:
 * 1. Validate conversation exists and requester is a member (SECURITY)
 * 2. Fetch messages with cursor-based pagination from MongoDB
 * 3. Batch-load sender profiles (anti-corruption layer)
 * 4. Compute bi-directional cursors (older/newer navigation)
 * 5. Return application-level projection (NOT REST DTOs)
 */
@Service
@RequiredArgsConstructor
public class GetMessagesHandler {

    private final MessageRepository      messageRepository;
    private final ConversationRepository conversationRepository;
    private final UserQueryPort          userQueryPort;

    @Transactional(readOnly = true)
    public MessageListResult getMessages(GetMessagesCommand command) {
        // 1. Validate conversation exists
        Conversation conversation = conversationRepository.findById(command.conversationId());
        if (conversation == null) {
            throw new AppException(ErrorCode.CONVERSATION_NOT_FOUND);
        }

        // 2. SECURITY: Assert requester is an active member
        conversation.assertMember(command.requesterId());

        // 3. Fetch messages (request limit+1 to detect hasMore)
        int fetchSize = command.limit() + 1;
        List<Message> messages = messageRepository.findByConversation(
                command.conversationId().value().toString(),
                command.cursor(),
                command.direction(),
                fetchSize);

        boolean hasMore = messages.size() > command.limit();
        if (hasMore) {
            messages = messages.subList(0, command.limit());
        }

        // 4. Ensure consistent order: always return oldest → newest
        if (command.direction() == CursorDirection.OLDER) {
            messages = messages.reversed();
        }

        // 5. Batch-load sender profiles
        Set<Long> senderIds = messages.stream()
                .map(m -> m.getSenderId().value())
                .collect(Collectors.toSet());
        Map<Long, UserInfo> userInfoMap = userQueryPort.findAllByIds(senderIds);

        // 6. Map to application projections
        List<MessageProjection> items = messages.stream()
                .map(m -> toProjection(m, userInfoMap))
                .toList();

        // 7. Compute bi-directional cursors
        String nextCursor = null;
        String prevCursor = null;

        if (!messages.isEmpty()) {
            Message firstMessage = messages.getFirst();
            Message lastMessage = messages.getLast();

            if (command.cursor() == null) {
                // First page: can scroll older (next = older direction)
                nextCursor = new MessageCursor(
                        firstMessage.getSequenceNumber(),
                        firstMessage.getId().value().toString()).encode();
            } else if (command.direction() == CursorDirection.OLDER) {
                // Scrolling older: nextCursor = go even older, prevCursor = go newer
                nextCursor = hasMore
                        ? new MessageCursor(firstMessage.getSequenceNumber(),
                                firstMessage.getId().value().toString()).encode()
                        : null;
                prevCursor = new MessageCursor(
                        lastMessage.getSequenceNumber(),
                        lastMessage.getId().value().toString()).encode();
            } else {
                // Scrolling newer: nextCursor = go even newer, prevCursor = go older
                prevCursor = new MessageCursor(
                        firstMessage.getSequenceNumber(),
                        firstMessage.getId().value().toString()).encode();
                nextCursor = hasMore
                        ? new MessageCursor(lastMessage.getSequenceNumber(),
                                lastMessage.getId().value().toString()).encode()
                        : null;
            }
        }

        return new MessageListResult(
                command.conversationId().value().toString(),
                items,
                nextCursor,
                prevCursor,
                items.size());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PRIVATE MAPPING
    // ═══════════════════════════════════════════════════════════════════════

    private MessageProjection toProjection(Message message, Map<Long, UserInfo> userInfoMap) {
        UserInfo senderInfo = userInfoMap.get(message.getSenderId().value());

        SenderInfo sender = senderInfo != null
                ? new SenderInfo(senderInfo.userId(), senderInfo.displayName(), senderInfo.avatarUrl())
                : new SenderInfo(message.getSenderId().value(), "Unknown", null);

        ContentInfo content = null;
        if (message.getContent() != null && !message.isRevoked()) {
            List<AttachmentInfo> attachments = message.getContent().attachments().stream()
                    .map(a -> new AttachmentInfo(
                            a.fileId(), a.fileName(), a.mimeType(),
                            a.fileSize(), a.url(), a.thumbnailUrl()))
                    .toList();
            content = new ContentInfo(message.getContent().text(), attachments);
        }

        ReplyInfo replyTo = null;
        if (message.isReply()) {
            // Note: for full reply info, we'd need to load the replied message
            // For now, we include the ID for client-side resolution
            replyTo = new ReplyInfo(
                    message.getReplyToMessageId().value(),
                    0L, null, null, null);
        }

        return new MessageProjection(
                message.getId().value(),
                message.getSequenceNumber(),
                message.getType().name(),
                sender,
                content,
                replyTo,
                message.getCreatedAt(),
                message.getEditedAt(),
                message.isRevoked());
    }
}
