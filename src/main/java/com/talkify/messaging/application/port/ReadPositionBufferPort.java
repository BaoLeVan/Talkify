package com.talkify.messaging.application.port;

import java.util.Map;
import java.util.Set;

public interface ReadPositionBufferPort {

    /**
     * Lấy toàn bộ buffer và xóa khỏi cache (atomic per conversation).
     *
     * @return conversationId → { userId → lastReadSeq }
     *         Chỉ chứa conversation có ít nhất 1 read event kể từ lần drain trước.
     */
    Set<Long>               collectActiveConversations();   // lấy + xóa active Set
    Map<Long, Long>         getReadPositions(Long convId);  // HGETALL, không xóa Hash

}
