package com.talkify.messaging.application.handler;

import org.springframework.stereotype.Service;

import com.talkify.messaging.application.command.TypingCommand;
import com.talkify.messaging.application.port.TypingPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TypingHandler {
    private final TypingPort typingPort;
    
    public void handleTypingEvent(TypingCommand command) {
        typingPort.publishTypingEvent(command.conversationId(), command.senderId(), command.isTyping());
    }
}
