package com.talkify.messaging.domain.model;

import java.util.Base64;

import com.talkify.common.exception.AppException;
import com.talkify.common.exception.ErrorCode;

public record MessageCursor(long sequenceNumber, String messageId) {

    public String encode() {
        return Base64.getUrlEncoder().encodeToString(
            (sequenceNumber + ":" + messageId).getBytes());
    }

    public static MessageCursor decode(String raw) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(raw));
            String[] parts = decoded.split(":", 2);
            return new MessageCursor(Long.parseLong(parts[0]), parts[1]);
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
    }
}