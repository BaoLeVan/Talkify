package com.talkify.messaging.interfaces.rest.response;

import java.util.List;

/**
 * REST response DTO for paginated conversation list.
 * 
 * This record is a simple data carrier — the Assembler handles construction.
 * No domain imports here (clean layer boundary).
 */
public record ConversationPageResponse(
    List<ConversationSummaryResponse> items,
    String nextCursor,
    int size
) {}

