package com.talkify.messaging.interfaces.rest.assembler;

import java.util.List;

import org.springframework.stereotype.Component;

import com.talkify.messaging.application.dto.ConversationListResult;
import com.talkify.messaging.application.dto.ConversationListResult.ConversationSummary;
import com.talkify.messaging.application.dto.ConversationListResult.MessagePreview;
import com.talkify.messaging.application.dto.ConversationListResult.PeerInfo;
import com.talkify.messaging.domain.model.ConversationType;
import com.talkify.messaging.domain.model.MessageType;
import com.talkify.messaging.interfaces.rest.response.ConversationPageResponse;
import com.talkify.messaging.interfaces.rest.response.ConversationSummaryResponse;
import com.talkify.messaging.interfaces.rest.response.MessagePreviewResponse;
import com.talkify.messaging.interfaces.rest.response.PeerResponse;

/**
 * ASSEMBLER — converts application-layer projections to REST response DTOs.
 * 
 * WHY this exists (DDD Interfaces layer pattern):
 * - The Application layer returns domain-oriented projections (ConversationListResult)
 * - The REST layer needs HTTP-specific DTOs (ConversationPageResponse)
 * - This assembler bridges the gap without polluting either layer
 * 
 * Benefits:
 * - Application layer never imports from interfaces (clean dependency direction)
 * - If we add GraphQL, gRPC, or WebSocket, each gets its own assembler
 * - REST response format can change without touching business logic
 */
@Component
public class ConversationAssembler {

    public ConversationPageResponse toPageResponse(ConversationListResult result) {
        List<ConversationSummaryResponse> items = result.items().stream()
                .map(this::toSummaryResponse)
                .toList();

        return new ConversationPageResponse(items, result.nextCursor(), result.size());
    }

    private ConversationSummaryResponse toSummaryResponse(ConversationSummary summary) {
        PeerResponse peer = summary.peer() != null
                ? toPeerResponse(summary.peer())
                : null;

        MessagePreviewResponse preview = toPreviewResponse(summary.preview());

        return new ConversationSummaryResponse(
                summary.id(),
                ConversationType.valueOf(summary.type()),
                summary.displayName(),
                summary.avatarUrl(),
                peer,
                preview,
                summary.lastMessageAt(),
                summary.unreadCount());
    }

    private PeerResponse toPeerResponse(PeerInfo peer) {
        return new PeerResponse(peer.userId(), peer.displayName(), peer.avatarUrl());
    }

    private MessagePreviewResponse toPreviewResponse(MessagePreview preview) {
        if (preview == null || preview.type() == null) {
            return MessagePreviewResponse.empty();
        }
        return new MessagePreviewResponse(
                MessageType.valueOf(preview.type()),
                preview.text(),
                preview.sentByMe(),
                preview.senderName());
    }
}
