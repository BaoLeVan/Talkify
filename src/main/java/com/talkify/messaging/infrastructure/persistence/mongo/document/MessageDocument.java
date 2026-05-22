package com.talkify.messaging.infrastructure.persistence.mongo.document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document(collection = "messages")
@CompoundIndexes({
    @CompoundIndex(name = "idx_conv_seq",     def = "{'conversationId': 1, 'sequenceNumber': -1}"),
    @CompoundIndex(name = "idx_conv_time",    def = "{'conversationId': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "idx_sender_time",  def = "{'senderId': 1, 'createdAt': -1}")
})
public class MessageDocument {

    @Id
    private Long id;                        // Snowflake ID

    @Field("conversationId")
    private String conversationId;

    @Field("sequenceNumber")
    private long sequenceNumber;

    @Field("senderId")
    private Long senderId;

    @Field("type")
    private String type;                    // TEXT | IMAGE | FILE | AUDIO | VIDEO | SYSTEM

    @Field("content")
    private ContentEmbedded content;

    @Field("replyToMessageId")
    private Long replyToMessageId;          // nullable

    @Field("revoked")
    private boolean revoked;

    @Field("revokedAt")
    private Instant revokedAt;

    @Field("editedAt")
    private Instant editedAt;

    @Field("createdAt")
    private Instant createdAt;

    // ── Embedded documents ────────────────────────────────────────────────

    @Getter @Setter
    public static class ContentEmbedded {
        private String text;
        private List<AttachmentEmbedded> attachments;
        private Map<String, Object> metadata;
    }

    @Getter @Setter
    public static class AttachmentEmbedded {
        private String fileId;
        private String fileName;
        private String mimeType;
        private long   fileSize;
        private String url;
        private String thumbnailUrl;
    }
}
