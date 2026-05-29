package com.talkify.messaging.application.handler;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.talkify.common.domain.UserId;
import com.talkify.messaging.application.port.ReadPositionBufferPort;
import com.talkify.messaging.application.port.ReadPositionPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlushReadPositionHandler {

    private final ReadPositionBufferPort bufferPort;
    private final ReadPositionPort       readPositionPort;

    public void flush() {
        Set<Long> activeConversations = bufferPort.collectActiveConversations();

        if (activeConversations.isEmpty()) {
            log.debug("No read positions to flush");
            return;
        }

        List<Long> sortedConvIds = activeConversations.stream()
            .sorted()
            .collect(Collectors.toList());

        int totalRows = 0;
        for (Long conversationId : sortedConvIds) {
            Map<Long, Long> userSeqMap = bufferPort.getReadPositions(conversationId);

            Map<UserId, Long> positions = userSeqMap.entrySet().stream()
                .sorted(Comparator.comparingLong(Map.Entry::getKey))
                .collect(Collectors.toMap(
                    e -> UserId.of(e.getKey()),
                    Map.Entry::getValue
                ));

            readPositionPort.batchUpsert(positions, conversationId);
            totalRows += positions.size();
        }

        log.info("Flushed {} read positions across {} conversations",
            totalRows, sortedConvIds.size());
    }
}
