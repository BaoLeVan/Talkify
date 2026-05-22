package com.talkify.messaging.interfaces.rest.response;

import java.util.List;

public record MessagePageResponse(
    String conversationId,
    List<MessageResponse> messages,
    String nextCursor,
    String prevCursor,
    int size
) {} 
