package com.talkify.contact.infrastructure.persistence.adapter;

import java.util.UUID;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.talkify.common.domain.UserId;
import com.talkify.contact.domain.exception.ContactNotFoundException;
import com.talkify.contact.domain.model.Contact;
import com.talkify.contact.domain.repository.ContactRepository;
import com.talkify.contact.infrastructure.persistence.entity.ContactJpaEntity;
import com.talkify.contact.infrastructure.persistence.mapper.ContactMapper;
import com.talkify.contact.infrastructure.persistence.repository.ContactJpaRepository;

import lombok.RequiredArgsConstructor;


@Repository
@RequiredArgsConstructor
public class ContactRepositoryAdapter implements ContactRepository {
    private final ContactJpaRepository contactJpaRepository;

    @Override
    public void save(Contact contact) {
        ContactJpaEntity contactEntity = contactJpaRepository.findByPublicId(contact.getId())
            .orElseGet(() -> {
                ContactJpaEntity newEntity = ContactMapper.toEntity(contact);
                return contactJpaRepository.save(newEntity);
            });
        contactEntity.setStatus(contact.getStatus().name());
        contactJpaRepository.save(contactEntity);
    }

    @Override
    public void delete(Contact contact) {
        ContactJpaEntity contactEntity = contactJpaRepository.findByPublicId(contact.getId())
            .orElseThrow(() -> new ContactNotFoundException());
        contactJpaRepository.delete(contactEntity);
    }

    @Override
    public Optional<Contact> findByUsers(UserId requesterId, UserId addresseeId) {
        Optional<ContactJpaEntity> contactJpaEntity = contactJpaRepository.findByRequesterIdAndAddresseeId(requesterId.value(), addresseeId.value());
        return contactJpaEntity.map(ContactMapper::toDomain);
    }

    @Override
    public Optional<Contact> findById(UUID id) {
        Optional<ContactJpaEntity> contactJpaEntity = contactJpaRepository.findByPublicId(id);
        return contactJpaEntity.map(ContactMapper::toDomain);
    }
}
