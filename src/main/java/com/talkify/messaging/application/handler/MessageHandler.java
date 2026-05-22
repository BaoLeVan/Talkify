package com.talkify.messaging.application.handler;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.talkify.common.domain.UserId;
import com.talkify.common.exception.AppException;
import com.talkify.common.exception.ErrorCode;
import com.talkify.common.id.IdGenerator;
import com.talkify.messaging.application.command.SendMessageCommand;
import com.talkify.messaging.application.port.MessagePublisher;
import com.talkify.messaging.domain.model.Conversation;
import com.talkify.messaging.domain.model.ConversationId;
import com.talkify.messaging.domain.model.Message;
import com.talkify.messaging.domain.model.MessageContent;
import com.talkify.messaging.domain.port.DomainEventPublisher;
import com.talkify.messaging.domain.port.SequenceGenerator;
import com.talkify.messaging.domain.repository.ConversationRepository;
import com.talkify.messaging.domain.repository.MessageRepository;

import lombok.RequiredArgsConstructor;

/**
 * USE CASE: Send a message to a conversation.
 * 
 * Orchestration responsibilities (Application layer):
 * 1. Resolve or create the target conversation
 * 2. Validate authorization (sender is member, conversation not suspended)
 * 3. Generate atomic sequence number via SequenceGenerator port
 * 4. Delegate message creation to Message aggregate (domain logic)
 * 5. Persist message (MongoDB) and update conversation snapshot (PostgreSQL)
 * 6. Dispatch domain events (for eventual consistency)
 * 7. Publish message for real-time delivery (future Kafka/WebSocket)
 * 
 * Transaction boundary: PostgreSQL update is transactional.
 * MongoDB save is NOT in the same transaction (eventual consistency by design).
 * If MongoDB fails after Postgres commit → compensating action needed (future).
 */
@Service
@RequiredArgsConstructor
public class MessageHandler {

    private final MessageRepository      messageRepository;
    private final ConversationRepository conversationRepository;
    private final IdGenerator            idGenerator;
    private final SequenceGenerator      sequenceGenerator;
    private final DomainEventPublisher   domainEventPublisher;
    private final MessagePublisher       messagePublisher;

    @Transactional
    public void handleSendMessage(SendMessageCommand command) {
        // 1. Resolve target conversation (find existing or create DIRECT)
        Conversation conversation = resolveConversation(command);

        // 2. Validate domain invariants
        UserId senderId = UserId.of(command.senderId());
        conversation.assertMember(senderId);
        conversation.assertNotSuspended();

        // 3. Atomic sequence allocation (Redis INCR — distributed-safe)
        long sequenceNumber = sequenceGenerator.nextSequence(conversation.getId().value());

        // 4. Create message via domain factory (enforces content validation)
        Message message = Message.create(
                idGenerator,
                conversation.getId(),
                senderId,
                command.messageType(),
                new MessageContent(command.text(), command.attachments(), Map.of()),
                sequenceNumber,
                command.replyToMessageId());

        // 5. Persist message to MongoDB
        messageRepository.save(message);

        // 6. Update conversation snapshot (denormalized for list query performance)
        //    Uses primitives to avoid cross-aggregate dependency
        conversation.updateLastMessage(
                message.getId(),
                message.getSenderId(),
                message.getType(),
                message.getContent().preview(100),
                message.getCreatedAt(),
                message.getSequenceNumber());
        conversationRepository.update(conversation);

        // 7. Dispatch domain events AFTER successful persistence
        domainEventPublisher.publishAll(message.pullDomainEvents());
        domainEventPublisher.publishAll(conversation.pullDomainEvents());

        // 8. Publish for real-time delivery (fire-and-forget, non-transactional)
        messagePublisher.publish(message);
    }

    /**
     * Resolve the target conversation:
     * - If conversationId provided → load existing
     * - If recipientId provided → find or create DIRECT conversation
     */
    private Conversation resolveConversation(SendMessageCommand command) {
        if (command.conversationId() != null) {
            Conversation conv = conversationRepository.findById(
                    ConversationId.of(command.conversationId()));
            if (conv == null) {
                throw new AppException(ErrorCode.CONVERSATION_NOT_FOUND);
            }
            return conv;
        }

        UserId sender    = UserId.of(command.senderId());
        UserId recipient = UserId.of(command.recipientId());

        return conversationRepository
                .findDirectBetween(sender, recipient)
                .orElseGet(() -> conversationRepository.save(
                        Conversation.createDirect(idGenerator, sender, recipient)));
    }
}
