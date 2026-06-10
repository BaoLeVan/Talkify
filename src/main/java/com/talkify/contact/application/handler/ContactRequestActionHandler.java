package com.talkify.contact.application.handler;

import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.talkify.common.domain.UserId;
import com.talkify.common.exception.AppException;
import com.talkify.common.exception.ErrorCode;
import com.talkify.contact.domain.exception.ContactNotFoundException;
import com.talkify.contact.domain.model.Contact;
import com.talkify.contact.domain.model.ContactStatus;
import com.talkify.contact.domain.repository.ContactRepository;
import com.talkify.contact.infrastructure.persistence.entity.ContactJpaEntity;
import com.talkify.contact.infrastructure.persistence.mapper.ContactMapper;
import com.talkify.contact.infrastructure.persistence.repository.ContactJpaRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ContactRequestActionHandler {
    private final ContactRepository contactRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void handleAcceptance(String requestId, UserId currentUserId) {
        UUID contactId = parseRequestId(requestId);
        Contact contact = getContact(contactId);
        
        contact.acceptRequest(currentUserId);
        contactRepository.save(contact);
        contact.pullDomainEvents().forEach(eventPublisher::publishEvent);
    }

    @Transactional
    public void handleRejection(String requestId, UserId currentUserId) {
        UUID contactId = parseRequestId(requestId);
        Contact contact = getContact(contactId);
        
        contact.rejectRequest(currentUserId);
        contactRepository.delete(contact);
        contact.pullDomainEvents().forEach(eventPublisher::publishEvent);
    }

    @Transactional
    public void handleCancellation(String requestId, UserId currentUserId) {
        UUID contactId = parseRequestId(requestId);
        Contact contact = getContact(contactId);
        
        contact.cancelRequest(currentUserId);
        contactRepository.delete(contact);
        contact.pullDomainEvents().forEach(eventPublisher::publishEvent);
    }

    private UUID parseRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        try {
            return UUID.fromString(requestId);
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
    }

    private Contact getContact(UUID contactId) {
        return contactRepository.findById(contactId)
            .orElseThrow(() -> new ContactNotFoundException());
    }
}
