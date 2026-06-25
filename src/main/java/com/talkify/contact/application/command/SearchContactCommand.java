package com.talkify.contact.application.command;

import com.talkify.common.domain.UserId;

public record SearchContactCommand(UserId userId, String query, int page, int size) {}
