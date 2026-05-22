package com.talkify.messaging.interfaces.rest.response;

import java.util.List;

public record MessageContentResponse(
    String text,
    List<AttachmentResponse> attachments
) {
    public static MessageContentResponse of(String text, List<AttachmentResponse> attachments) {
        return new MessageContentResponse(text, attachments);
    }
}
