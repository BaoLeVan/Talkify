package com.talkify.messaging.infrastructure.persistence.mapper;

import com.talkify.common.domain.UserId;
import com.talkify.messaging.domain.model.*;
import com.talkify.messaging.infrastructure.persistence.entity.ConversationJpaEntity;
import com.talkify.messaging.infrastructure.persistence.entity.ParticipantJpaEntity;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ConversationJpaMapper {

    // Entity → Domain
    public static Conversation toDomain(ConversationJpaEntity entity) {
        Set<Participant> participants = entity.getParticipants().stream()
            .map(ParticipantMapper::toParticipant)
            .collect(Collectors.toSet());

        return Conversation.restore(
            new ConversationId(entity.getId()),
            entity.getTitle(),
            ConversationType.valueOf(entity.getType().name()),
            ConversationStatus.valueOf(entity.getStatus()),
            entity.getAvatarUrl(),
            participants,
            entity.getLastMessageId() != null ? new MessageId(entity.getLastMessageId()) : null,
            entity.getLastMessageSenderId() != null ? new UserId(entity.getLastMessageSenderId()) : null,
            entity.getLastMessageType(),
            entity.getLastMessagePreview(),
            entity.getLastMessageAt(),
            entity.getSequenceCounter(),
            entity.getCreatedAt(),
            new UserId(entity.getCreatedBy()),
            entity.getUpdatedAt(),
            entity.getUpdatedBy() != null ? new UserId(entity.getUpdatedBy()) : null
        );
    }

    // Domain → Entity
    public static ConversationJpaEntity toEntity(Conversation domain) {
        ConversationJpaEntity entity = new ConversationJpaEntity();
        entity.setId(domain.getId().value());
        entity.setTitle(domain.getTitle());
        entity.setType(domain.getType());
        entity.setStatus(domain.getStatus().name());
        entity.setAvatarUrl(domain.getAvatarUrl());
        entity.setLastMessageId(
            domain.getLastMessageId() != null ? domain.getLastMessageId().value() : null
        );
        entity.setLastMessageSenderId(
            domain.getLastMessageSenderId() != null ? domain.getLastMessageSenderId().value() : null
        );
        entity.setLastMessageType(domain.getLastMessageType());
        entity.setLastMessagePreview(domain.getLastMessagePreview());
        entity.setLastMessageAt(domain.getLastMessageAt());
        entity.setSequenceCounter(domain.getSequenceCounter());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setCreatedBy(domain.getCreatedBy().value());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setUpdatedBy(
            domain.getUpdatedBy() != null ? domain.getUpdatedBy().value() : null
        );

        Set<ParticipantJpaEntity> participants = domain.getParticipants().stream()
            .map(p -> ParticipantMapper.toParticipantEntity(p, domain.getId().value()))
            .collect(Collectors.toSet());
        entity.setParticipants(participants);

        return entity;
    }

    // Chỉ cập nhật scalar fields trên entity đã được JPA quản lý (có participants với ID rồi)
    public static void updateScalars(ConversationJpaEntity entity, Conversation domain) {
        entity.setTitle(domain.getTitle());
        entity.setStatus(domain.getStatus().name());
        entity.setAvatarUrl(domain.getAvatarUrl());
        entity.setLastMessageId(
            domain.getLastMessageId() != null ? domain.getLastMessageId().value() : null
        );
        entity.setLastMessageSenderId(
            domain.getLastMessageSenderId() != null ? domain.getLastMessageSenderId().value() : null
        );
        entity.setLastMessageType(domain.getLastMessageType());
        entity.setLastMessagePreview(domain.getLastMessagePreview());
        entity.setLastMessageAt(domain.getLastMessageAt());
        entity.setSequenceCounter(domain.getSequenceCounter());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setUpdatedBy(
            domain.getUpdatedBy() != null ? domain.getUpdatedBy().value() : null
        );
    }
}