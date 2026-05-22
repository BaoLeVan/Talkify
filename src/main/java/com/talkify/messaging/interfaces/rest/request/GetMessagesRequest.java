package com.talkify.messaging.interfaces.rest.request;

import com.talkify.messaging.domain.model.CursorDirection;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record GetMessagesRequest(
    String cursor,
    CursorDirection direction,
    @Min(1) @Max(100) int size
) {
    public GetMessagesRequest(String cursor, CursorDirection direction, Integer size) {
        this(cursor,
             direction != null ? direction : CursorDirection.OLDER,
             size      != null ? size      : 30);
    }
}