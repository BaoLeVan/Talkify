package com.talkify.common.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.talkify.messaging.domain.event.DomainEvent;

/**
 * Base class for Aggregate Roots in DDD.
 * 
 * Responsibilities:
 * 1. Collect domain events during state transitions
 * 2. Expose events for dispatching AFTER persistence (transactional outbox pattern)
 * 
 * Usage pattern:
 *   aggregate.doSomething();           // registers events internally
 *   repository.save(aggregate);        // persist state
 *   eventPublisher.publishAll(aggregate.pullDomainEvents()); // dispatch events
 * 
 * IMPORTANT: pullDomainEvents() clears the internal list — call only once after save.
 */
public abstract class AggregateRoot {

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    /**
     * Register a domain event to be published after this aggregate is persisted.
     * Called from within domain methods (state transitions).
     */
    protected void registerEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    /**
     * Pull all accumulated domain events and clear the internal buffer.
     * Called by the application layer AFTER successful persistence.
     * 
     * @return unmodifiable list of events in registration order
     */
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = Collections.unmodifiableList(new ArrayList<>(domainEvents));
        domainEvents.clear();
        return events;
    }

    /**
     * Check if there are pending domain events (useful for testing).
     */
    public boolean hasDomainEvents() {
        return !domainEvents.isEmpty();
    }
}