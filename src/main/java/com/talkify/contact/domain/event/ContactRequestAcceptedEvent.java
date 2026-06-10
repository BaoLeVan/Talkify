package com.talkify.contact.domain.event;

import com.talkify.common.domain.DomainEvent;

public record ContactRequestAcceptedEvent(
    String contactId,
    long requesterId,
    long addresseeId
) implements DomainEvent {
    @Override
    public java.time.Instant occurredAt() {
        return java.time.Instant.now();
    }
}
