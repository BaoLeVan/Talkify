package com.talkify.common.domain;

import java.time.Instant;

/**
 * Marker interface for all domain events in the Messaging bounded context.
 * 
 * Every domain event must carry:
 * - occurredAt: when the event happened (wall-clock time)
 * 
 * This contract enables:
 * 1. Type-safe event collection in AggregateRoot
 * 2. Future event serialization for Kafka/Event Store
 * 3. Polymorphic dispatching in DomainEventPublisher
 */
public interface DomainEvent {
    Instant occurredAt();
}
