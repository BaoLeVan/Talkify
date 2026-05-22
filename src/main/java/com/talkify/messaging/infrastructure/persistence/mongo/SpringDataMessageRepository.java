package com.talkify.messaging.infrastructure.persistence.mongo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.talkify.messaging.infrastructure.persistence.mongo.document.MessageDocument;

public interface SpringDataMessageRepository extends MongoRepository<MessageDocument, Long> {

    Optional<MessageDocument> findById(Long id);

    // Cursor-based pagination — lấy messages có sequenceNumber < beforeSeq, mới nhất trước
    List<MessageDocument> findByConversationIdAndSequenceNumberLessThanOrderBySequenceNumberDesc(
        String conversationId, long beforeSequence
    );

    List<MessageDocument> findByConversationIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
        String conversationId, long afterSequence
    );
}
