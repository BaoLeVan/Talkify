package com.talkify.messaging.interfaces.rest.assembler;

import java.util.List;

import org.springframework.stereotype.Component;

import com.talkify.messaging.application.dto.MessageListResult;
import com.talkify.messaging.application.dto.MessageListResult.AttachmentInfo;
import com.talkify.messaging.application.dto.MessageListResult.MessageProjection;
import com.talkify.messaging.domain.model.MessageType;
import com.talkify.messaging.interfaces.rest.response.AttachmentResponse;
import com.talkify.messaging.interfaces.rest.response.MessageContentResponse;
import com.talkify.messaging.interfaces.rest.response.MessagePageResponse;
import com.talkify.messaging.interfaces.rest.response.MessageResponse;
import com.talkify.messaging.interfaces.rest.response.ReplyToResponse;
import com.talkify.messaging.interfaces.rest.response.SenderResponse;

/**
 * ASSEMBLER — converts MessageListResult (application DTO) to REST response.
 */
@Component
public class MessageAssembler {

    public MessagePageResponse toPageResponse(MessageListResult result) {
        List<MessageResponse> messages = result.items().stream()
                .map(this::toMessageResponse)
                .toList();

        return new MessagePageResponse(
                result.conversationId(),
                messages,
                result.nextCursor(),
                result.prevCursor(),
                result.size());
    }

    private MessageResponse toMessageResponse(MessageProjection projection) {
        SenderResponse sender = new SenderResponse(
                String.valueOf(projection.sender().userId()),
                projection.sender().displayName(),
                projection.sender().avatarUrl());

        MessageContentResponse content = null;
        if (projection.content() != null) {
            List<AttachmentResponse> attachments = projection.content().attachments().stream()
                    .map(this::toAttachmentResponse)
                    .toList();
            content = new MessageContentResponse(projection.content().text(), attachments);
        }

        ReplyToResponse replyTo = null;
        if (projection.replyTo() != null) {
            var r = projection.replyTo();
            replyTo = new ReplyToResponse(
                    r.messageId(), r.senderId(), r.senderName(),
                    r.type() != null ? MessageType.valueOf(r.type()) : null,
                    r.previewText());
        }

        return new MessageResponse(
                projection.id(),
                projection.sequenceNumber(),
                MessageType.valueOf(projection.type()),
                sender,
                content,
                replyTo,
                projection.sentAt(),
                projection.editedAt());
    }

    private AttachmentResponse toAttachmentResponse(AttachmentInfo a) {
        return new AttachmentResponse(
                a.fileId(), a.fileName(), a.mimeType(),
                a.fileSize(), a.url(), a.thumbnailUrl());
    }
}
