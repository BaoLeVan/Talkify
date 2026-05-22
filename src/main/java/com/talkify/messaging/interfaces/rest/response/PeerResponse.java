package com.talkify.messaging.interfaces.rest.response;

/**
 * Thông tin đối phương trong cuộc trò chuyện DIRECT.
 * Null cho GROUP conversations.
 */
public record PeerResponse(
    long userId,
    String displayName,
    String avatarUrl
) {}
