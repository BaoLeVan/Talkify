package com.talkify.messaging.application.port;

public interface ReadReceiptPublishPort {
    void bufferReadReceipt(Long conversationId, Long userId, Long sequenceNumber);
    void publishReadReceipt(Long conversationId, long readerid, Long sequenceNumber, long readAt);
}
