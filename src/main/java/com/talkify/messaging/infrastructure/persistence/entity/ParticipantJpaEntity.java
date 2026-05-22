package com.talkify.messaging.infrastructure.persistence.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(
    name = "conversation_members",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_conversation_user",
        columnNames = {"conversations_id", "users_id"}
    )
)
public class ParticipantJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversations_id", nullable = false)
    private ConversationJpaEntity conversation;

    @Column(name = "users_id", nullable = false)
    private Long userId;

    // conversations_id đã được map qua @JoinColumn ở trên — KHÔNG khai báo lại

    @Column(nullable = false)
    private String role;

    @Column
    private String nickname;

    @Column(name = "last_read_sequence", nullable = false)
    private long lastReadSequence;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
