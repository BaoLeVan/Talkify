package com.talkify.messaging.application.handler;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.talkify.common.domain.UserId;
import com.talkify.common.domain.UserInfo;
import com.talkify.messaging.application.command.GetConversationsCommand;
import com.talkify.messaging.application.dto.ConversationListResult;
import com.talkify.messaging.application.dto.ConversationListResult.ConversationSummary;
import com.talkify.messaging.application.dto.ConversationListResult.MessagePreview;
import com.talkify.messaging.application.dto.ConversationListResult.PeerInfo;
import com.talkify.messaging.application.port.UserQueryPort;
import com.talkify.messaging.domain.model.Conversation;
import com.talkify.messaging.domain.model.ConversationCursor;
import com.talkify.messaging.domain.model.ConversationType;
import com.talkify.messaging.domain.model.MessageType;
import com.talkify.messaging.domain.model.Participant;
import com.talkify.messaging.domain.repository.ConversationRepository;

import lombok.RequiredArgsConstructor;

/**
 * QUERY USE CASE: Get paginated conversation list for the current user.
 * 
 * Responsibilities:
 * 1. Load conversations with cursor-based pagination
 * 2. Batch-load user info (anti-corruption layer) to avoid N+1
 * 3. Compute display data (name, avatar, preview, unread count)
 * 4. Return application-level projections (NOT REST DTOs)
 * 
 * The interfaces layer (Assembler) converts these projections to REST format.
 * This maintains the layer boundary: Application → Domain only, never → Interfaces.
 */
@Service
@RequiredArgsConstructor
public class ConversationHandler {

    private final ConversationRepository conversationRepository;
    private final UserQueryPort          userQueryPort;

    @Transactional(readOnly = true)
    public ConversationListResult getConversations(GetConversationsCommand command) {
        // 1. Load conversations page
        List<Conversation> conversations = conversationRepository.findByParticipantId(
                command.userId(), command.cursor(), command.size());

        // 2. Batch-load user info (avoid N+1 queries)
        Map<Long, UserInfo> userInfoCache = loadUserInfoBatch(conversations, command.userId());

        long currentUserId = command.userId().value();

        // 3. Map to application-level projections
        List<ConversationSummary> items = conversations.stream()
                .map(c -> toSummary(c, currentUserId, userInfoCache))
                .toList();

        // 4. Compute next cursor for pagination
        String nextCursor = computeNextCursor(conversations, command.size());

        return new ConversationListResult(items, nextCursor, items.size());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private Map<Long, UserInfo> loadUserInfoBatch(List<Conversation> conversations, UserId currentUserId) {
        Set<Long> userIds = conversations.stream()
                .flatMap(c -> {
                    var ids = new java.util.HashSet<Long>();
                    if (c.getType() == ConversationType.DIRECT) {
                        c.findPeer(currentUserId)
                                .ifPresent(peer -> ids.add(peer.getUserId().value()));
                    }
                    if (c.getType() == ConversationType.GROUP && c.getLastMessageSenderId() != null) {
                        ids.add(c.getLastMessageSenderId().value());
                    }
                    return ids.stream();
                })
                .collect(Collectors.toSet());

        return userQueryPort.findAllByIds(userIds);
    }

    private ConversationSummary toSummary(Conversation c, long currentUserId,
                                          Map<Long, UserInfo> userInfoCache) {
        String displayName;
        String avatarUrl;
        PeerInfo peer;

        if (c.getType() == ConversationType.DIRECT) {
            Optional<Participant> peerOpt = c.findPeer(UserId.of(currentUserId));

            if (peerOpt.isPresent()) {
                Participant peerParticipant = peerOpt.get();
                UserInfo peerInfo = userInfoCache.get(peerParticipant.getUserId().value());
                String nickname = peerParticipant.getNickname();

                displayName = (nickname != null && !nickname.isBlank())
                        ? nickname
                        : (peerInfo != null ? peerInfo.displayName() : "Unknown");
                avatarUrl = peerInfo != null ? peerInfo.avatarUrl() : null;
                peer = peerInfo != null
                        ? new PeerInfo(peerInfo.userId(), peerInfo.displayName(), peerInfo.avatarUrl())
                        : null;
            } else {
                displayName = "Unknown";
                avatarUrl = null;
                peer = null;
            }
        } else {
            displayName = c.getTitle();
            avatarUrl = c.getAvatarUrl();
            peer = null;
        }

        MessagePreview preview = buildPreview(c, currentUserId, userInfoCache);
        long unreadCount = c.computeUnreadCount(UserId.of(currentUserId));

        return new ConversationSummary(
                c.getId().value(),
                c.getType().name(),
                displayName,
                avatarUrl,
                peer,
                preview,
                c.getLastMessageAt(),
                unreadCount);
    }

    private MessagePreview buildPreview(Conversation c, long currentUserId,
                                        Map<Long, UserInfo> userInfoCache) {
        MessageType type = c.getLastMessageType();
        if (type == null) {
            return MessagePreview.empty();
        }

        boolean sentByMe = c.getLastMessageSenderId() != null
                && c.getLastMessageSenderId().value() == currentUserId;
        String text = type == MessageType.TEXT ? c.getLastMessagePreview() : null;
        String senderName = resolveSenderName(c, currentUserId, sentByMe, userInfoCache);

        return new MessagePreview(type.name(), text, sentByMe, senderName);
    }

    private String resolveSenderName(Conversation c, long currentUserId, boolean sentByMe,
                                     Map<Long, UserInfo> userInfoCache) {
        // In DIRECT chats, sender name is implicit (it's you or the peer)
        if (c.getType() == ConversationType.DIRECT) return null;
        // If you sent it, no need to display your own name
        if (sentByMe) return null;
        if (c.getLastMessageSenderId() == null) return null;

        UserInfo senderInfo = userInfoCache.get(c.getLastMessageSenderId().value());
        return senderInfo != null ? senderInfo.displayName() : "Unknown";
    }

    private String computeNextCursor(List<Conversation> conversations, int requestedSize) {
        if (conversations.size() < requestedSize) {
            return null; // No more pages
        }
        Conversation last = conversations.getLast();
        Instant ts = last.getLastMessageAt() != null ? last.getLastMessageAt() : last.getCreatedAt();
        return new ConversationCursor(ts, last.getId().value()).encode();
    }
}