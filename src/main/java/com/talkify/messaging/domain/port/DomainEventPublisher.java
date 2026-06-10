package com.talkify.messaging.domain.port;

import java.util.List;

import com.talkify.common.domain.DomainEvent;

/**
 * Outbound port — Domain layer defines the contract for event publishing.
 * Infrastructure provides the adapter (Spring ApplicationEventPublisher, Kafka, etc.)
 * 
 * DDD Rule: Domain never depends on infrastructure. This port inverts the dependency
 * so that the domain can emit events without knowing HOW they are delivered.
 * 
 * Adapter implementations:
 * - SpringDomainEventPublisher: dispatch via Spring's ApplicationEventPublisher (monolith phase)
 * - KafkaDomainEventPublisher: publish to Kafka topics (microservices phase)
 */
public interface DomainEventPublisher {

    /**
     * Publish a single domain event.
     */
    void publish(DomainEvent event);

    /**
     * Publish all events collected from an aggregate after persistence.
     * Default implementation delegates to single publish in order.
     */
    default void publishAll(List<DomainEvent> events) {
        events.forEach(this::publish);
    }
}
