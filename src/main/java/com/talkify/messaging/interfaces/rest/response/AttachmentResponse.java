package com.talkify.messaging.interfaces.rest.response;

public record AttachmentResponse (
    String fileId,
    String fileName,
    String mimeType,
    long fileSize,
    String url,
    String thumbnailUrl
) {}
