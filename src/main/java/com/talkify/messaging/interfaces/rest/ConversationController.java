package com.talkify.messaging.interfaces.rest;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.talkify.common.security.SecurityUtils;
import com.talkify.dto.response.ApiResponse;
import com.talkify.messaging.application.command.GetConversationsCommand;
import com.talkify.messaging.application.command.GetMessagesCommand;
import com.talkify.messaging.application.dto.ConversationListResult;
import com.talkify.messaging.application.dto.MessageListResult;
import com.talkify.messaging.application.handler.ConversationHandler;
import com.talkify.messaging.application.handler.GetMessagesHandler;
import com.talkify.messaging.domain.model.ConversationCursor;
import com.talkify.messaging.domain.model.ConversationId;
import com.talkify.messaging.domain.model.CursorDirection;
import com.talkify.messaging.domain.model.MessageCursor;
import com.talkify.messaging.interfaces.rest.assembler.ConversationAssembler;
import com.talkify.messaging.interfaces.rest.assembler.MessageAssembler;
import com.talkify.messaging.interfaces.rest.request.GetConversationsRequest;
import com.talkify.messaging.interfaces.rest.request.GetMessagesRequest;
import com.talkify.messaging.interfaces.rest.response.ConversationPageResponse;
import com.talkify.messaging.interfaces.rest.response.MessagePageResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST Controller — Conversation & Messages endpoints.
 * 
 * Responsibilities (THIN controller):
 * 1. Parse HTTP request → validate → build Command/Query
 * 2. Delegate to Application layer (use cases)
 * 3. Convert application result → REST response via Assembler
 * 
 * NO business logic here. Controller is a technical adapter.
 */
@Validated
@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationHandler    conversationHandler;
    private final GetMessagesHandler     getMessagesHandler;
    private final ConversationAssembler  conversationAssembler;
    private final MessageAssembler       messageAssembler;

    @GetMapping
    public ApiResponse<ConversationPageResponse> getConversations(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {

        @Valid GetConversationsRequest request = new GetConversationsRequest(cursor, size);

        ConversationCursor parsedCursor = request.cursor() != null
                ? ConversationCursor.decode(request.cursor())
                : null;

        GetConversationsCommand command = new GetConversationsCommand(
                SecurityUtils.requireCurrentUserId(),
                parsedCursor,
                request.size());

        // Application layer returns domain projection; Assembler converts to REST DTO
        ConversationListResult result = conversationHandler.getConversations(command);
        return ApiResponse.ok(conversationAssembler.toPageResponse(result));
    }

    @GetMapping("/{id}/messages")
    public ApiResponse<MessagePageResponse> getMessages(
            @PathVariable("id") Long conversationId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) CursorDirection direction,
            @RequestParam(required = false) Integer size) {

        @Valid GetMessagesRequest request = new GetMessagesRequest(cursor, direction, size);

        MessageCursor parsedCursor = request.cursor() != null
                ? MessageCursor.decode(request.cursor())
                : null;

        GetMessagesCommand command = new GetMessagesCommand(
                ConversationId.of(conversationId),
                SecurityUtils.requireCurrentUserId(),
                parsedCursor,
                request.direction(),
                request.size());

        // Application layer returns domain projection; Assembler converts to REST DTO
        MessageListResult result = getMessagesHandler.getMessages(command);
        return ApiResponse.ok(messageAssembler.toPageResponse(result));
    }
}
