package com.talkify.messaging.domain.repository;

import java.util.List;
import java.util.Optional;

import com.talkify.messaging.domain.model.CursorDirection;
import com.talkify.messaging.domain.model.Message;
import com.talkify.messaging.domain.model.MessageCursor;
import com.talkify.messaging.domain.model.MessageId;

public interface MessageRepository {
    void save(Message message);
    Message findById(MessageId messageId);
    Optional<Message> findOptionalById(MessageId messageId);
    List<Message> findByConversation(String conversationId, MessageCursor cursor, CursorDirection direction, int limit);
    List<Message> findByConversationBeforeSequence(String conversationId, long beforeSequence, int limit);
    List<Message> findByConversationAfterSequence(String conversationId, long afterSequence, int limit);
}
