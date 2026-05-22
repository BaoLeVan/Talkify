package com.talkify.messaging.interfaces.rest.response;

import com.talkify.messaging.domain.model.MessageType;

public record ReplyToResponse(
    long messageId,
    long senderId,
    String senderName,
    MessageType type,
    String previewText
) {
    
}
