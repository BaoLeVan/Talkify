package com.talkify.messaging.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.talkify.common.domain.UserId;
import com.talkify.messaging.domain.model.Conversation;
import com.talkify.messaging.domain.model.ConversationCursor;
import com.talkify.messaging.domain.model.ConversationId;
import com.talkify.messaging.domain.repository.ConversationRepository;
import com.talkify.messaging.infrastructure.persistence.entity.ConversationJpaEntity;
import com.talkify.messaging.infrastructure.persistence.mapper.ConversationJpaMapper;
import com.talkify.messaging.infrastructure.persistence.repository.ConversationJpaRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ConversationRepositoryAdapter implements ConversationRepository {

    private final ConversationJpaRepository conversationJpaRepository;

    @Override
    @Transactional
    public Conversation save(Conversation conversation) {
        ConversationJpaEntity entity = ConversationJpaMapper.toEntity(conversation);
        ConversationJpaEntity saved = conversationJpaRepository.save(entity);
        return ConversationJpaMapper.toDomain(saved);
    }

    @Override
    @Transactional
    public void update(Conversation conversation) {
        conversationJpaRepository.findById(conversation.getId().value())
            .ifPresent(entity -> ConversationJpaMapper.updateScalars(entity, conversation));
        // entity is managed by JPA — dirty checking will flush the UPDATE automatically
    }

    @Override
    @Transactional(readOnly = true)
    public Conversation findById(ConversationId conversationId) {
        return conversationJpaRepository.findByIdWithParticipants(conversationId.value())
            .map(ConversationJpaMapper::toDomain)
            .orElse(null);
    }

    @Override
    public List<Conversation> findByParticipantId(UserId userId, ConversationCursor cursor, int limit) {
        var pageable = PageRequest.of(0, limit);
        List<ConversationJpaEntity> entities;
        if (cursor == null) {
            entities = conversationJpaRepository.findByUserIdFirstPage(userId.value(), pageable);
        } else {
            entities = conversationJpaRepository.findByUserIdAfterCursor(
                    userId.value(), cursor.lastMessageAt(), cursor.conversationId(), pageable);
        }
        return entities.stream().map(ConversationJpaMapper::toDomain).toList();
    }

    @Override
    public Optional<Conversation> findDirectBetween(UserId user1, UserId user2) {
        return conversationJpaRepository
            .findDirectBetween(user1.value(), user2.value())
            .map(ConversationJpaMapper::toDomain);
    }
}
