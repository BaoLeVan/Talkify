package com.talkify.messaging.application.port;

import java.util.Map;
import java.util.Optional;

import com.talkify.common.domain.UserId;

public interface ReadPositionPort {
    Optional<Long> findLastReadSequence(Long conversationId, Long userId);
    Map<Long, Long> findAllReadPositions(Long conversationId);
    void batchUpsert(Map<UserId, Long> readPositions, Long conversationId);
}
