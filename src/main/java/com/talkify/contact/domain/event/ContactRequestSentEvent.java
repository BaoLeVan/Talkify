package com.talkify.contact.domain.event;

import java.time.Instant;
import com.talkify.common.domain.DomainEvent;

public record ContactRequestSentEvent (
    String contactId,
    long requesterId,
    long addresseeId,
    Instant occurredAt
) implements DomainEvent {
    public ContactRequestSentEvent(String contactId, long requesterId, long addresseeId) {
        this(contactId, requesterId, addresseeId, Instant.now());
    }   
}
