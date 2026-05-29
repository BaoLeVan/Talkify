package com.talkify.messaging.application.handler;

import org.springframework.stereotype.Service;

import com.talkify.messaging.application.command.MarkAsReadCommand;
import com.talkify.messaging.application.port.ReadReceiptPublishPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReadReceipHandler {
    private final ReadReceiptPublishPort readReceiptPort;

    public void handleReadReceipt(MarkAsReadCommand command) {
        Long conversationId = command.conversationId();
        Long userId = command.userId();
        Long sequenceNumber = command.sequenceNumber();

        readReceiptPort.bufferReadReceipt(conversationId, userId, sequenceNumber);
        readReceiptPort.publishReadReceipt(conversationId, userId, sequenceNumber, System.currentTimeMillis());
    }
}
