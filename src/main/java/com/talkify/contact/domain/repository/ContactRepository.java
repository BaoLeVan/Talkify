package com.talkify.contact.domain.repository;

import java.util.UUID;
import java.util.Optional;

import com.talkify.common.domain.UserId;
import com.talkify.contact.domain.model.Contact;

public interface ContactRepository {
    void save(Contact contact);
    void delete(Contact contact);
    Optional<Contact> findById(UUID id);
    Optional<Contact> findByUsers(UserId requesterId, UserId addresseeId);
}
