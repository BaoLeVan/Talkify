package com.talkify.messaging.application.handler;

import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.talkify.common.domain.UserId;
import com.talkify.common.exception.AppException;
import com.talkify.common.exception.ErrorCode;
import com.talkify.common.id.IdGenerator;
import com.talkify.messaging.application.command.SendMessageCommand;
import com.talkify.messaging.application.event.MessageDispatchEvent;
import com.talkify.messaging.domain.model.Conversation;
import com.talkify.messaging.domain.model.ConversationId;
import com.talkify.messaging.domain.model.Message;
import com.talkify.messaging.domain.model.MessageContent;
import com.talkify.messaging.domain.port.DomainEventPublisher;
import com.talkify.messaging.domain.port.SequenceGenerator;
import com.talkify.messaging.domain.repository.ConversationRepository;
import com.talkify.messaging.domain.repository.MessageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageHandler {

    private final MessageRepository           messageRepository;
    private final ConversationRepository      conversationRepository;
    private final IdGenerator                 idGenerator;
    private final SequenceGenerator           sequenceGenerator;
    private final DomainEventPublisher        domainEventPublisher;
    private final ApplicationEventPublisher   applicationEventPublisher;

    @Transactional
    public void handleSendMessage(SendMessageCommand command) {
        Conversation conversation = resolveConversation(command);

        UserId senderId = UserId.of(command.senderId());
        conversation.assertMember(senderId);
        conversation.assertNotSuspended();

        long sequenceNumber = sequenceGenerator.nextSequence(conversation.getId().value());

        Message message = Message.create(
                idGenerator,
                conversation.getId(),
                senderId,
                command.messageType(),
                new MessageContent(command.text(), command.attachments(), Map.of()),
                sequenceNumber,
                command.replyToMessageId());

        messageRepository.save(message);

        conversation.updateLastMessage(
                message.getId(),
                message.getSenderId(),
                message.getType(),
                message.getContent().preview(100),
                message.getCreatedAt(),
                message.getSequenceNumber());
        conversationRepository.update(conversation);

        domainEventPublisher.publishAll(message.pullDomainEvents());
        domainEventPublisher.publishAll(conversation.pullDomainEvents());

        applicationEventPublisher.publishEvent(buildDispatchEvent(message, conversation));
    }

    private MessageDispatchEvent buildDispatchEvent(Message message, Conversation conversation) {
        List<MessageDispatchEvent.AttachmentPayload> attachments = message.getContent().attachments()
                .stream()
                .map(a -> new MessageDispatchEvent.AttachmentPayload(
                        a.mimeType(), a.url(), a.fileName(), a.fileSize()))
                .toList();

        MessageDispatchEvent.ReplyPayload replyTo = message.isReply()
                ? new MessageDispatchEvent.ReplyPayload(
                        message.getReplyToMessageId().value().toString(),
                        message.getContent().preview(50),
                        message.getType().name())
                : null;

        List<Long> recipientIds = conversation.getParticipants().stream()
                .filter(p -> p.isActive())
                .map(p -> p.getUserId().value())
                .toList();

        return new MessageDispatchEvent(
                conversation.getId().value().toString(),
                conversation.getType().name(),
                recipientIds,
                message.getId().value().toString(),
                message.getSequenceNumber(),
                message.getSenderId().value(),
                message.getType().name(),
                message.getContent().text(),
                attachments,
                replyTo,
                message.getCreatedAt().toString());
    }

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

