package com.talkify.messaging.application.port;

public interface TypingPort {

    void publishTypingEvent(Long conversationId, Long userId, boolean isTyping);
}
