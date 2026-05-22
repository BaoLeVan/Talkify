package com.talkify.messaging.domain.repository;

import java.util.List;
import java.util.Optional;

import com.talkify.common.domain.UserId;
import com.talkify.messaging.domain.model.Conversation;
import com.talkify.messaging.domain.model.ConversationCursor;
import com.talkify.messaging.domain.model.ConversationId;

public interface ConversationRepository {
    /** Persist a brand-new conversation (INSERT). */
    Conversation save(Conversation conversation);

    /** Update scalar fields of an existing conversation (UPDATE, no participant changes). */
    void update(Conversation conversation);

    Conversation findById(ConversationId conversationId);

    /**
     * Cursor-based list of conversations for a user, sorted by lastMessageAt DESC.
     * cursor = null → first page (no lower bound).
     */
    List<Conversation> findByParticipantId(UserId userId, ConversationCursor cursor, int limit);

    /** Tìm DIRECT conversation đang active giữa 2 user, trả về empty nếu chưa có. */
    Optional<Conversation> findDirectBetween(UserId user1, UserId user2);
}
