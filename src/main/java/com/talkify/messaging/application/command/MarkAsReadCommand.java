package com.talkify.messaging.application.command;

public record MarkAsReadCommand(Long conversationId, Long userId, Long sequenceNumber) {}
