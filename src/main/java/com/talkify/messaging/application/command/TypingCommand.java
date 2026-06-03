package com.talkify.messaging.application.command;

public record TypingCommand(
    Long conversationId,
    Long senderId,
    boolean isTyping
) {}