package com.talkify.messaging.application.dto;

import java.time.Instant;
import java.util.List;

/**
 * Application-layer projection for paginated conversation list.
 * 
 * This lives in the APPLICATION layer (not interfaces) because:
 * - The application layer needs a way to return query results
 * - It should NOT import REST-specific DTOs (ConversationPageResponse)
 * - The interfaces layer maps this to the final REST response format
 * 
 * This decouples: Domain → Application DTO → REST Response
 */
public record ConversationListResult(
    List<ConversationSummary> items,
    String nextCursor,
    int size
) {

    /**
     * Projection of a single conversation for the list view.
     * Contains pre-computed display data (unread count, preview, peer info).
     */
    public record ConversationSummary(
        long id,
        String type,
        String displayName,
        String avatarUrl,
        PeerInfo peer,
        MessagePreview preview,
        Instant lastMessageAt,
        long unreadCount
    ) {}

    public record PeerInfo(
        long userId,
        String displayName,
        String avatarUrl
    ) {}

    public record MessagePreview(
        String type,
        String text,
        boolean sentByMe,
        String senderName
    ) {
        public static MessagePreview empty() {
            return new MessagePreview(null, null, false, null);
        }
    }
}
