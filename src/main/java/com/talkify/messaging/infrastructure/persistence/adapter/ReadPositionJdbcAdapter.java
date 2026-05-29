package com.talkify.messaging.infrastructure.persistence.adapter;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.talkify.common.domain.UserId;
import com.talkify.messaging.application.port.CachePort;
import com.talkify.messaging.application.port.ReadPositionPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReadPositionJdbcAdapter implements ReadPositionPort {

    private final JdbcTemplate jdbcTemplate;
    private final CachePort cachePort;

    private static final String UPDATE_SQL = """
        UPDATE conversation_members
           SET last_read_sequence = GREATEST(last_read_sequence, ?),
               updated_at         = NOW()
         WHERE conversations_id = ?
           AND users_id         = ?
        """;

    /**
     * Batch update last_read_sequence cho nhiều user trong 1 conversation.
     * Tất cả rows gửi trong 1 batch → 1 round-trip DB.
     *
     * @param readPositions userId → lastReadSeq
     * @param conversationId conversation cần update
     */
    @Override
    public void batchUpsert(Map<UserId, Long> readPositions, Long conversationId) {
        if (readPositions.isEmpty()) return;

        // Sort theo userId để lock order nhất quán trong cùng 1 conversation
        List<Object[]> args = readPositions.entrySet().stream()
            .sorted(Comparator.comparingLong(e -> e.getKey().value()))
            .map(e -> new Object[]{ e.getValue(), conversationId, e.getKey().value() })
            // params theo thứ tự ? trong SQL: seq, conversations_id, users_id
            .collect(Collectors.toList());

        int[] results = jdbcTemplate.batchUpdate(UPDATE_SQL, args);

        long updated = 0;
        for (int r : results) updated += r;
        log.debug("batchUpsert conv={}: {} rows affected out of {} attempted",
            conversationId, updated, args.size());
    }

    /**
     * Tìm last_read_sequence của 1 user trong 1 conversation.
     * Dùng để tính unread count: unread = sequence_counter - last_read_sequence.
     */
    @Override
    public Optional<Long> findLastReadSequence(Long conversationId, Long userId) {
        List<Long> rows = jdbcTemplate.queryForList(
            """
            SELECT last_read_sequence
              FROM conversation_members
             WHERE conversations_id = ?
               AND users_id         = ?
            """,
            Long.class, conversationId, userId);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    /**
     * Lấy toàn bộ read position trong 1 conversation.
     * Dùng để hiển thị trạng thái đã đọc của từng member.
     *
     * @return userId → lastReadSeq
     */
    @Override
    public Map<Long, Long> findAllReadPositions(Long conversationId) {
        Map<Long, Long> cached = cachePort.getReadReceiptsForConversation(conversationId);
        if (cached != null && !cached.isEmpty()) {
            log.debug("Cache hit for read positions of conv={}", conversationId);
            return cached;
        }
        return jdbcTemplate.query(
            """
            SELECT users_id, last_read_sequence
              FROM conversation_members
             WHERE conversations_id = ?
               AND left_at IS NULL
            """,
            (rs, rowNum) -> Map.entry(
                rs.getLong("users_id"),
                rs.getLong("last_read_sequence")),
            conversationId
        ).stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
