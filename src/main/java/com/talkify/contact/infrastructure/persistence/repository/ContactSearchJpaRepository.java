package com.talkify.contact.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.talkify.contact.domain.model.Contact;
import com.talkify.contact.domain.port.ContactSearchRepository;
import com.talkify.contact.infrastructure.persistence.mapper.ContactMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ContactSearchJpaRepository implements ContactSearchRepository {
    private final ContactJpaRepository contactJpaRepository;

    @Override
    public Optional<Contact> searchByEmail(String email) {
        return contactJpaRepository.findByEmail(email)
            .map(ContactMapper::toDomain);
    }

    @Override
    public Optional<Contact> searchByPhoneNumber(String phoneNumber) {
        return contactJpaRepository.findByPhoneNumber(phoneNumber)
            .map(ContactMapper::toDomain);
    }

    @Override
    public Optional<Contact> searchByUsername(String username) {
        return contactJpaRepository.findByUsername(username)
            .map(ContactMapper::toDomain);
    }
}
