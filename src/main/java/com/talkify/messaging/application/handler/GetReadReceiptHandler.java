package com.talkify.messaging.application.handler;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.talkify.messaging.application.port.ReadPositionPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetReadReceiptHandler {
    private final ReadPositionPort readPositionPort;

    public Map<Long, Long> handle(Long conversationId) {
        return readPositionPort.findAllReadPositions(conversationId);
    }
}
