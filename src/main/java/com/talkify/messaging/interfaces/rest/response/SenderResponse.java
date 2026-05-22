package com.talkify.messaging.interfaces.rest.response;

/**
 * REST response DTO for message sender info.
 * No domain imports — just a data carrier.
 */
public record SenderResponse(
    String userId,
    String displayName,
    String avatarUrl
) {}
