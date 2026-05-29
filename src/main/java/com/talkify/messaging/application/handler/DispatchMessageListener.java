package com.talkify.messaging.application.handler;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.talkify.messaging.application.event.MessageDispatchEvent;
import com.talkify.messaging.application.port.MessageDispatchPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DispatchMessageListener {

    private final MessageDispatchPort messageDispatchPort;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageDispatched(MessageDispatchEvent event) {
        messageDispatchPort.dispatch(event);
    }
}
