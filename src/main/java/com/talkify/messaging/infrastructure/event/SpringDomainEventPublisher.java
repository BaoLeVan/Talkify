package com.talkify.messaging.infrastructure.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.talkify.messaging.domain.event.DomainEvent;
import com.talkify.messaging.domain.port.DomainEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring ApplicationEvent-based implementation of DomainEventPublisher.
 * 
 * Current phase (monolith): dispatches events via Spring's in-process event bus.
 * Spring @EventListener / @TransactionalEventListener methods can subscribe.
 * 
 * Microservices phase: Replace this with KafkaDomainEventPublisher that serializes
 * events to Kafka topics. The domain layer won't change at all (Ports & Adapters).
 * 
 * Usage pattern:
 *   // In use case (after persistence):
 *   domainEventPublisher.publishAll(aggregate.pullDomainEvents());
 * 
 * Subscribers can use:
 *   @EventListener
 *   public void on(MessageSentEvent event) { ... }
 * 
 *   @TransactionalEventListener(phase = AFTER_COMMIT)
 *   public void onAfterCommit(MessageSentEvent event) { ... }
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(DomainEvent event) {
        log.debug("Publishing domain event: {}", event.getClass().getSimpleName());
        applicationEventPublisher.publishEvent(event);
    }
}
