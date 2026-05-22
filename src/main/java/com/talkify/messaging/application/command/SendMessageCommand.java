package com.talkify.messaging.application.command;

import java.util.List;

import com.talkify.messaging.domain.model.Attachment;
import com.talkify.messaging.domain.model.MessageId;
import com.talkify.messaging.domain.model.MessageType;

public record SendMessageCommand(
    Long conversationId,
    Long recipientId,
    long senderId,
    MessageType messageType,
    String text,
    List<Attachment> attachments,
    MessageId replyToMessageId
) {}
