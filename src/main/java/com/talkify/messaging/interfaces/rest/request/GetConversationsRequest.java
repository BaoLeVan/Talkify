package com.talkify.messaging.interfaces.rest.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Interface-layer DTO — owns format validation and default values.
 * cursor: opaque Base64 token from previous response; null = first page.
 */
public record GetConversationsRequest(
    String cursor,

    @Min(value = 1, message = "size must be >= 1")
    @Max(value = 100, message = "size must be <= 100")
    int size
) {}
