package com.talkify.contact.infrastructure.persistence.entity;

import java.util.UUID;

import com.github.f4b6a3.uuid.UuidCreator;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "contacts")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class ContactJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "contact_seq")
    @SequenceGenerator(name = "contact_seq", sequenceName = "contact_sequence", allocationSize = 1)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Column(name = "addressee_id", nullable = false)
    private Long addresseeId;

    @Column(name = "status", nullable = false)
    private String status;

    
    @PrePersist
    public void prePersist() {
        if (publicId == null) {
            publicId = UuidCreator.getTimeOrderedEpoch();
        }
    }

}
