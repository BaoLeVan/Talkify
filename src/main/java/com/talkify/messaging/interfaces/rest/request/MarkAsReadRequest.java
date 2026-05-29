package com.talkify.messaging.interfaces.rest.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MarkAsReadRequest (
    @NotNull @Min(value = 1, message = "sequenceNumber must be > 0")
    Long sequenceNumber
) {}
