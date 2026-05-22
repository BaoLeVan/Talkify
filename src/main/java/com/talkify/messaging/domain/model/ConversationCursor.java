package com.talkify.messaging.domain.model;

import java.time.Instant;
import java.util.Base64;

/**
 * Opaque cursor for conversation list pagination.
 * Encodes (lastMessageAt epoch millis, conversationId) as Base64 so clients
 * treat it as a black box and cannot construct arbitrary cursors.
 *
 * Sort order: lastMessageAt DESC, id DESC (tiebreaker)
 * Wire format: Base64Url("<epochMillis>:<conversationId>")
 */
public record ConversationCursor(Instant lastMessageAt, long conversationId) {

    public String encode() {
        String raw = lastMessageAt.toEpochMilli() + ":" + conversationId;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes());
    }

    public static ConversationCursor decode(String encoded) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(encoded));
            String[] parts = raw.split(":");
            if (parts.length != 2) throw new IllegalArgumentException();
            Instant ts = Instant.ofEpochMilli(Long.parseLong(parts[0]));
            long id    = Long.parseLong(parts[1]);
            return new ConversationCursor(ts, id);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid cursor: " + encoded, e);
        }
    }
}

