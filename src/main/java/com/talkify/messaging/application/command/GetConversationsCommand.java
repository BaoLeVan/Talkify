package com.talkify.messaging.application.command;

import com.talkify.common.domain.UserId;
import com.talkify.messaging.domain.model.ConversationCursor;

public record GetConversationsCommand(
    UserId userId,
    ConversationCursor cursor,  // null → first page
    int size
) {}
