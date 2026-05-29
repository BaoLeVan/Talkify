package com.talkify.messaging.infrastructure.adapter;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.talkify.messaging.application.event.MessageDispatchEvent;
import com.talkify.messaging.infrastructure.kafka.KafkaConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaToWebsocketAdapter {

    private final SimpMessagingTemplate messageTemplate;

    @KafkaListener(topics = KafkaConfig.CHAT_TOPIC, groupId = "${spring.kafka.consumer.group-id}")
    public void consume(MessageDispatchEvent event) {
        if (event.recipientIds() == null || event.recipientIds().isEmpty()) {
            log.warn("No recipients | msgId={}", event.messageId());
            return;
        }

        switch (event.conversationType()) {
            case "GROUP"  -> dispatchGroup(event);
            case "DIRECT" -> dispatchDirect(event);
            default -> log.warn("Unknown conversationType={} | msgId={}",
                    event.conversationType(), event.messageId());
        }
    }

    private void dispatchGroup(MessageDispatchEvent event) {
        String destination = "/topic/conversations/" + event.conversationId();
        messageTemplate.convertAndSend(destination, event);
        log.debug("WS GROUP | dest={} msgId={}", destination, event.messageId());
    }

    private void dispatchDirect(MessageDispatchEvent event) {
        for (Long userId : event.recipientIds()) {
            messageTemplate.convertAndSendToUser(userId.toString(), "/queue/messages", event);
            log.debug("WS DIRECT | userId={} msgId={}", userId, event.messageId());
        }
    }
}
