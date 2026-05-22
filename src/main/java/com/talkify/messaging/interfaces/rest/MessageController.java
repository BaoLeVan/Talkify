package com.talkify.messaging.interfaces.rest;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.talkify.common.security.AuthPrincipal;
import com.talkify.dto.response.ApiResponse;
import com.talkify.messaging.application.command.SendMessageCommand;
import com.talkify.messaging.application.handler.MessageHandler;
import com.talkify.messaging.interfaces.rest.request.SendMessageRequest;
import com.talkify.messaging.interfaces.rest.response.SendMessageResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {
    private final MessageHandler messageHandler;
    
    @PostMapping()
    public ApiResponse<Void> sendMessage(
        @Valid @RequestBody SendMessageRequest request,
        @AuthenticationPrincipal AuthPrincipal principal
    ) {
        SendMessageCommand command = new SendMessageCommand(
            request.conversationId(),
            request.recipientId(),
            principal.userId().value(),
            request.messageType(),
            request.text(),
            request.attachments(),
            request.replyToMessageId()
        );
        messageHandler.handleSendMessage(command);
        return ApiResponse.ok("Message sent successfully", null);
    }
}
