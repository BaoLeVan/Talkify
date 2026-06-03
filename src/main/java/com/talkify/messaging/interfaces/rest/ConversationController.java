package com.talkify.messaging.interfaces.rest;

import java.security.Principal;
import java.util.Map;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.talkify.common.exception.AppException;
import com.talkify.common.exception.ErrorCode;
import com.talkify.common.security.SecurityUtils;
import com.talkify.dto.response.ApiResponse;
import com.talkify.messaging.application.command.GetConversationsCommand;
import com.talkify.messaging.application.command.GetMessagesCommand;
import com.talkify.messaging.application.command.MarkAsReadCommand;
import com.talkify.messaging.application.command.TypingCommand;
import com.talkify.messaging.application.dto.ConversationListResult;
import com.talkify.messaging.application.dto.MessageListResult;
import com.talkify.messaging.application.handler.ConversationHandler;
import com.talkify.messaging.application.handler.GetMessagesHandler;
import com.talkify.messaging.application.handler.GetReadReceiptHandler;
import com.talkify.messaging.application.handler.ReadReceipHandler;
import com.talkify.messaging.application.handler.TypingHandler;
import com.talkify.messaging.domain.model.ConversationCursor;
import com.talkify.messaging.domain.model.ConversationId;
import com.talkify.messaging.domain.model.CursorDirection;
import com.talkify.messaging.domain.model.MessageCursor;
import com.talkify.messaging.interfaces.rest.assembler.ConversationAssembler;
import com.talkify.messaging.interfaces.rest.assembler.MessageAssembler;
import com.talkify.messaging.interfaces.rest.request.GetConversationsRequest;
import com.talkify.messaging.interfaces.rest.request.GetMessagesRequest;
import com.talkify.messaging.interfaces.rest.request.MarkAsReadRequest;
import com.talkify.messaging.interfaces.rest.response.ConversationPageResponse;
import com.talkify.messaging.interfaces.rest.response.GetReadReceiptsResponse;
import com.talkify.messaging.interfaces.rest.response.MessagePageResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationHandler    conversationHandler;
    private final GetMessagesHandler     getMessagesHandler;
    private final ConversationAssembler  conversationAssembler;
    private final MessageAssembler       messageAssembler;
    private final ReadReceipHandler      readReceipHandler;
    private final GetReadReceiptHandler  getReadReceiptHandler;
    private final TypingHandler          typingHandler;

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

    @PostMapping("/{conversationId}/read")
    public ApiResponse<Void> markAsRead(
        @PathVariable("conversationId") Long conversationId,
        @RequestBody MarkAsReadRequest request
    ) {
        MarkAsReadCommand command = new MarkAsReadCommand(
            conversationId,
            SecurityUtils.requireCurrentUserId().value(),
            request.sequenceNumber()
        );
        readReceipHandler.handleReadReceipt(command);
        return ApiResponse.ok("Conversation marked as read", null);
    }

    @GetMapping("/{conversationId}/read-receipts")
    public ApiResponse<GetReadReceiptsResponse> getReadReceipts(
        @PathVariable("conversationId") Long conversationId
    ) {
        Map<Long, Long> readReceipts = getReadReceiptHandler.handle(conversationId);
        return ApiResponse.ok("Read receipts retrieved successfully", GetReadReceiptsResponse.from(readReceipts));
    }

    @MessageMapping("/conv/{conversationId}/typing")
    public void handleTyping(
        @DestinationVariable("conversationId") Long conversationId,
        TypingCommand command,
        Principal principal
    ) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        long userId = Long.parseLong(principal.getName());

        command = new TypingCommand(
            conversationId,
            userId,
            command.isTyping()
        );
        typingHandler.handleTypingEvent(command);
    }
}
