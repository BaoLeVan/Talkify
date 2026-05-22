package com.talkify.messaging.domain.model;

import java.time.Instant;

import com.talkify.common.domain.UserId;

import lombok.Getter;

@Getter
public class Participant {
    private UserId userId;
    private ConversationRole role;
    private long lastReadSequence;
    private String nickname;
    private Instant joinedAt;
    private Instant leftAt;

    public static Participant create(UserId userId, ConversationRole role) {
        Participant participant = new Participant();
        participant.userId = userId;
        participant.role = role;
        participant.nickname = null;
        participant.lastReadSequence = 0L;
        participant.joinedAt = Instant.now();
        return participant;
    }

    public static Participant restore(UserId userId, ConversationRole role, String nickname,
                                      long lastReadSequence, Instant joinedAt, Instant leftAt) {
        Participant participant = new Participant();
        participant.userId = userId;
        participant.role = role;
        participant.nickname = nickname;
        participant.lastReadSequence = lastReadSequence;
        participant.joinedAt = joinedAt;
        participant.leftAt = leftAt;
        return participant;
    }

    public static Participant owner(UserId userId) {
        return create(userId, ConversationRole.OWNER);
    }

    public static Participant member(UserId userId) {
        return create(userId, ConversationRole.MEMBER);
    }

    public boolean isActive() {
        return leftAt == null;
    }

    public boolean isAdmin() {
        return role == ConversationRole.ADMIN || role == ConversationRole.OWNER;
    }

    public boolean isOwner() {
        return role == ConversationRole.OWNER;
    }

    public void markAsRead(long sequence) {
        if (sequence > lastReadSequence) {
            lastReadSequence = sequence;
        }
    }

    public void leave() {
        this.leftAt = Instant.now();
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setRole(ConversationRole role) {
        this.role = role;
    }
}
