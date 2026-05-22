package com.talkify.messaging.application.command;

import com.talkify.common.domain.UserId;
import com.talkify.messaging.domain.model.ConversationId;
import com.talkify.messaging.domain.model.CursorDirection;
import com.talkify.messaging.domain.model.MessageCursor;

public record GetMessagesCommand(
    ConversationId conversationId,
    UserId       requesterId,
    MessageCursor cursor,
    CursorDirection direction,
    int limit
) {}
