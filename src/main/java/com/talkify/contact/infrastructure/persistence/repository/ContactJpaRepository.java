package com.talkify.contact.infrastructure.persistence.repository;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.talkify.contact.infrastructure.persistence.entity.ContactJpaEntity;

public interface ContactJpaRepository extends JpaRepository<ContactJpaEntity, Long> {
    Optional<ContactJpaEntity> findByRequesterIdAndAddresseeId(Long requesterId, Long addresseeId);
    Optional<ContactJpaEntity> findByPublicId(UUID publicId);
    Optional<ContactJpaEntity> findByEmail(String email);
    Optional<ContactJpaEntity> findByPhoneNumber(String phoneNumber);
    Optional<ContactJpaEntity> findByUsername(String username);
}
