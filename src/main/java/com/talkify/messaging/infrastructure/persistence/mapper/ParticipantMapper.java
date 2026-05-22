package com.talkify.messaging.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;

import com.talkify.common.domain.UserId;
import com.talkify.messaging.domain.model.ConversationRole;
import com.talkify.messaging.domain.model.Participant;
import com.talkify.messaging.infrastructure.persistence.entity.ParticipantJpaEntity;

@Component
public class ParticipantMapper {
    public static ParticipantJpaEntity toParticipantEntity(Participant participant, Long conversationId) {
        ParticipantJpaEntity member = new ParticipantJpaEntity();
        member.setUserId(participant.getUserId().value());
        member.setRole(participant.getRole().name());
        member.setNickname(participant.getNickname());
        member.setLastReadSequence(participant.getLastReadSequence());
        member.setJoinedAt(participant.getJoinedAt());
        member.setLeftAt(participant.getLeftAt());
        return member;
    }

    public static Participant toParticipant(ParticipantJpaEntity member) {
        return Participant.restore(
            new UserId(member.getUserId()),
            ConversationRole.valueOf(member.getRole()),
            member.getNickname(),
            member.getLastReadSequence(),
            member.getJoinedAt(),
            member.getLeftAt()
        );
    }
}
