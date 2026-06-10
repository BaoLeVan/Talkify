package com.talkify.contact.infrastructure.persistence.mapper;

import com.talkify.common.domain.UserId;
import com.talkify.contact.domain.model.Contact;
import com.talkify.contact.domain.model.ContactStatus;
import com.talkify.contact.infrastructure.persistence.entity.ContactJpaEntity;

public class ContactMapper {
    public static ContactJpaEntity toEntity(Contact contact) {
        ContactJpaEntity entity = ContactJpaEntity.builder()
            .publicId(contact.getId())
            .requesterId(contact.getRequesterId().value())
            .addresseeId(contact.getAddresseeId().value())
            .status(contact.getStatus().name())
            .build();
        return entity;
    }

    public static Contact toDomain(ContactJpaEntity entity) {
        Contact contact = Contact.reconstitute(
            entity.getPublicId(), 
            new UserId(entity.getRequesterId()), 
            new UserId(entity.getAddresseeId()), 
            ContactStatus.valueOf(entity.getStatus()));
        return contact;
    }
}
