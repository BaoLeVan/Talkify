package com.talkify.messaging.interfaces.rest.response;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SendMessageResponse {
    @JsonProperty("message_id")
    String messageId;

    @JsonProperty("conversation_id")
    String conversationId;

    @JsonProperty("sender_id")
    String senderId;

    @JsonProperty("type")
    String type;

    @JsonProperty("text")
    String text;

    @JsonProperty("attachments")
    List<AttachmentResponse> attachments;

    @JsonProperty("reply_to")
    ReplyToResponse replyTo;

    @JsonProperty("sequence_number")
    long sequenceNumber;

    @JsonProperty("status")
    String status;

    @JsonProperty("sent_at")
    Instant sentAt;

    @JsonProperty("edited_at")
    Instant editedAt;

    @JsonProperty("deleted_at")
    Instant deletedAt;
}
