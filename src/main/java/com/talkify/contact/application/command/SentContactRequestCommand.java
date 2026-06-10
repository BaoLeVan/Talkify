package com.talkify.contact.application.command;

public record SentContactRequestCommand(
    long requesterId,
    long addresseeId
) {}
