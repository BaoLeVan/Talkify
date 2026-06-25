package com.talkify.contact.application.strategy;

import java.util.Optional;

import com.talkify.contact.domain.model.Contact;

public interface ContactSearchStrategy {
     Optional<Contact> search(String keyword);
}
