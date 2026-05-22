package com.talkify.messaging.infrastructure.persistence.mongo.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.talkify.common.domain.UserId;
import com.talkify.messaging.domain.model.Attachment;
import com.talkify.messaging.domain.model.ConversationId;
import com.talkify.messaging.domain.model.Message;
import com.talkify.messaging.domain.model.MessageContent;
import com.talkify.messaging.domain.model.MessageId;
import com.talkify.messaging.domain.model.MessageType;
import com.talkify.messaging.infrastructure.persistence.mongo.document.MessageDocument;
import com.talkify.messaging.infrastructure.persistence.mongo.document.MessageDocument.AttachmentEmbedded;
import com.talkify.messaging.infrastructure.persistence.mongo.document.MessageDocument.ContentEmbedded;

@Component
public class MessageMongoMapper {

    // Domain → Document
    public MessageDocument toDocument(Message domain) {
        MessageDocument doc = new MessageDocument();
        doc.setId(domain.getId().value());
        doc.setConversationId(String.valueOf(domain.getConversationId().value()));
        doc.setSequenceNumber(domain.getSequenceNumber());
        doc.setSenderId(domain.getSenderId().value());
        doc.setType(domain.getType().name());
        doc.setReplyToMessageId(
            domain.getReplyToMessageId() != null ? domain.getReplyToMessageId().value() : null
        );
        doc.setRevoked(domain.isRevoked());
        doc.setRevokedAt(domain.getRevokedAt());
        doc.setEditedAt(domain.getEditedAt());
        doc.setCreatedAt(domain.getCreatedAt());

        if (domain.getContent() != null) {
            ContentEmbedded content = new ContentEmbedded();
            content.setText(domain.getContent().text());
            content.setMetadata(domain.getContent().metadata());
            content.setAttachments(
                domain.getContent().attachments().stream()
                    .map(this::toAttachmentEmbedded)
                    .toList()
            );
            doc.setContent(content);
        }

        return doc;
    }

    // Document → Domain (restore — không emit events)
    public Message toDomain(MessageDocument doc) {
        MessageContent content = null;
        if (doc.getContent() != null) {
            List<Attachment> attachments = doc.getContent().getAttachments() == null
                ? List.of()
                : doc.getContent().getAttachments().stream()
                    .map(this::toAttachment)
                    .toList();

            content = new MessageContent(
                doc.getContent().getText(),
                attachments,
                doc.getContent().getMetadata()
            );
        }

        return Message.restore(
            new MessageId(doc.getId()),
            new ConversationId(Long.parseLong(doc.getConversationId())),
            new UserId(doc.getSenderId()),
            MessageType.valueOf(doc.getType()),
            content,
            doc.getSequenceNumber(),
            doc.getReplyToMessageId() != null ? new MessageId(doc.getReplyToMessageId()) : null,
            doc.isRevoked(),
            doc.getRevokedAt(),
            doc.getEditedAt(),
            doc.getCreatedAt()
        );
    }

    private AttachmentEmbedded toAttachmentEmbedded(Attachment a) {
        AttachmentEmbedded emb = new AttachmentEmbedded();
        emb.setFileId(a.fileId());
        emb.setFileName(a.fileName());
        emb.setMimeType(a.mimeType());
        emb.setFileSize(a.fileSize());
        emb.setUrl(a.url());
        emb.setThumbnailUrl(a.thumbnailUrl());
        return emb;
    }

    private Attachment toAttachment(AttachmentEmbedded emb) {
        return new Attachment(
            emb.getFileId(),
            emb.getFileName(),
            emb.getMimeType(),
            emb.getFileSize(),
            emb.getUrl(),
            emb.getThumbnailUrl()
        );
    }
}
