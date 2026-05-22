package com.talkify.messaging.domain.model;

public record Attachment(
    String fileId,
    String fileName,
    String mimeType,
    long fileSize,
    String url,
    String thumbnailUrl
) {}
