package com.talkify.messaging.infrastructure.persistence.entity;

import java.util.HashSet;
import java.util.Set;

import com.talkify.messaging.domain.model.ConversationType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "conversations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConversationJpaEntity {

    @Id
    private Long id;

    @Column
    private String title;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ConversationType type;

    @Column(nullable = false)
    private String status;

    @Column(name = "last_message_id")
    private Long lastMessageId;

    @Column(name = "last_message_sender_id")
    private Long lastMessageSenderId;

    @Column(name = "last_message_type")
    @Enumerated(EnumType.STRING)
    private com.talkify.messaging.domain.model.MessageType lastMessageType;

    @Column(name = "last_message_preview")
    private String lastMessagePreview;

    @Column(name = "last_message_at")
    private java.time.Instant lastMessageAt;

    @Column(name = "sequence_counter")
    private Long sequenceCounter;

    @Column(name = "deleted_at")
    private java.time.Instant deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.Instant createdAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    @Column(name = "updated_at")
    private java.time.Instant updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @OneToMany(
        mappedBy = "conversation",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @Builder.Default
    private Set<ParticipantJpaEntity> participants = new HashSet<>();

    // Helper — dùng trong mapper
    public void setParticipants(Set<ParticipantJpaEntity> participants) {
        this.participants.clear();
        if (participants != null) {
            participants.forEach(p -> p.setConversation(this)); // giữ FK 2 chiều
            this.participants.addAll(participants);
        }
    }
}
