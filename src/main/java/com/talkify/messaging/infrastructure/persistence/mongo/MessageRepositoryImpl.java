package com.talkify.messaging.infrastructure.persistence.mongo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.talkify.common.exception.AppException;
import com.talkify.common.exception.ErrorCode;
import com.talkify.messaging.domain.model.CursorDirection;
import com.talkify.messaging.domain.model.Message;
import com.talkify.messaging.domain.model.MessageCursor;
import com.talkify.messaging.domain.model.MessageId;
import com.talkify.messaging.domain.repository.MessageRepository;
import com.talkify.messaging.infrastructure.persistence.mongo.document.MessageDocument;
import com.talkify.messaging.infrastructure.persistence.mongo.mapper.MessageMongoMapper;

import lombok.RequiredArgsConstructor;

/**
 * Implementation của domain port MessageRepository.
 * Đây là Adapter trong Hexagonal Architecture:
 *   Domain (port) ←── MessageRepositoryImpl (adapter) ──► MongoDB
 */
@Repository
@RequiredArgsConstructor
public class MessageRepositoryImpl implements MessageRepository {
    private final SpringDataMessageRepository springDataRepo;
    private final MessageMongoMapper          mapper;
    private final MongoTemplate               mongoTemplate;

    @Override
    public void save(Message message) {
        springDataRepo.save(mapper.toDocument(message));
    }

    @Override
    public Message findById(MessageId messageId) {
        return springDataRepo.findById(messageId.value())
            .map(mapper::toDomain)
            .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));
    }

    @Override
    public Optional<Message> findOptionalById(MessageId messageId) {
        return springDataRepo.findById(messageId.value())
            .map(mapper::toDomain);
    }

    @Override
    public List<Message> findByConversationBeforeSequence(
            String conversationId, long beforeSequence, int limit) {
        return springDataRepo
            .findByConversationIdAndSequenceNumberLessThanOrderBySequenceNumberDesc(
                conversationId, beforeSequence
            )
            .stream()
            .limit(limit)
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public List<Message> findByConversationAfterSequence(String conversationId, long afterSequence, int limit) {
        return springDataRepo
            .findByConversationIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
                conversationId, afterSequence
            )
            .stream()
            .limit(limit)
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public List<Message> findByConversation(String conversationId, MessageCursor cursor, CursorDirection direction,
            int limit) {
        Criteria criteria = Criteria.where("conversationId").is(conversationId);

        if (cursor != null) {
            if (direction == CursorDirection.OLDER) {
                criteria = criteria.and("sequenceNumber").lt(cursor.sequenceNumber());
            } else {
                criteria = criteria.and("sequenceNumber").gt(cursor.sequenceNumber());
            }
        }

         Sort sort = (direction == CursorDirection.NEWER)
                ? Sort.by(Sort.Direction.ASC, "sequenceNumber")
                : Sort.by(Sort.Direction.DESC, "sequenceNumber");

        Query query = new Query(criteria).with(sort).limit(limit);

        List<MessageDocument> docs = mongoTemplate.find(query, MessageDocument.class, "messages");
        return docs.stream().map(mapper::toDomain).toList(); 
    }
}
