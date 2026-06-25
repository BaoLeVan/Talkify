package com.talkify.contact.domain.port;

import java.util.Optional;

import com.talkify.contact.domain.model.Contact;

public interface ContactSearchRepository {
    Optional<Contact> searchByEmail(String email);
    Optional<Contact> searchByPhoneNumber(String phoneNumber);
    Optional<Contact> searchByUsername(String username);
}
