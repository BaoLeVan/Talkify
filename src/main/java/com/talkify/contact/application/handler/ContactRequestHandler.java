package com.talkify.contact.application.handler;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.talkify.common.domain.UserId;
import com.talkify.common.exception.AppException;
import com.talkify.common.exception.ErrorCode;
import com.talkify.common.id.UUIDv7Generator;
import com.talkify.contact.application.command.SentContactRequestCommand;
import com.talkify.contact.domain.model.Contact;
import com.talkify.contact.domain.model.ContactStatus;
import com.talkify.contact.domain.repository.ContactRepository;
import com.talkify.dto.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ContactRequestHandler {
    private final ContactRepository contactRepository;
    
    public ApiResponse<String> handle(SentContactRequestCommand command) {
        Optional<Contact> contact = contactRepository.findByUsers(new UserId(command.requesterId()), new UserId(command.addresseeId()));

        if (contact.isPresent()) {
            if (contact.get().getStatus() == ContactStatus.PENDING) {
                throw new AppException(ErrorCode.FRIEND_REQUEST_ALREADY_SENT);
            } else if (contact.get().getStatus() == ContactStatus.BLOCKED) {
                throw new AppException(ErrorCode.CONTACT_NOT_FOUND);
            }
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Contact newContact = Contact.createRequest(UUIDv7Generator.generate(), new UserId(command.requesterId()), new UserId(command.addresseeId()));
        contactRepository.save(newContact);

        return ApiResponse.ok("Friend request sent successfully");
    }
}
