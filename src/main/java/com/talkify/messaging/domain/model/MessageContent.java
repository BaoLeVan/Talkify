package com.talkify.messaging.domain.model;

import java.util.List;
import java.util.Map;

import com.talkify.common.exception.AppException;
import com.talkify.common.exception.ErrorCode;

public record MessageContent(
    String text,
    List<Attachment> attachments,
    Map<String, Object> metadata
) {
    public MessageContent {
        boolean hasText = text != null && !text.isBlank();
        boolean hasAttachment = attachments != null && !attachments.isEmpty();
        if (!hasText && !hasAttachment) {
            throw new AppException(ErrorCode.MESSAGE_CONTENT_EMPTY);
        }
        attachments = attachments != null ? List.copyOf(attachments) : List.of();
        metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    public String preview(int maxLength) {
        if (text != null && !text.isBlank()) {
            return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
        } else if (attachments != null && !attachments.isEmpty()) {
            return "Attachment";
        } else {
            return "No content";
        }
    }
}
