package com.talkify.contact.application.strategy;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.talkify.contact.domain.model.Contact;
import com.talkify.contact.domain.port.ContactSearchRepository;

@Component
public class UsernameContactSearch implements ContactSearchStrategy {
    private final ContactSearchRepository contactSearchRepository;

    public UsernameContactSearch(ContactSearchRepository contactSearchRepository) {
        this.contactSearchRepository = contactSearchRepository;
    }

    @Override
    public Optional<Contact> search(String keyword) {
        return contactSearchRepository.searchByUsername(keyword);
    }
    
}
