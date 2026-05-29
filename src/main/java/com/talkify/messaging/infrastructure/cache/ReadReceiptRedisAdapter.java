package com.talkify.messaging.infrastructure.cache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;
import com.talkify.messaging.application.port.ReadPositionBufferPort;
import com.talkify.messaging.application.port.ReadReceiptPublishPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Infrastructure adapter: implements cả 2 write-side ports liên quan đến Redis.
 *
 * ReadReceiptPublishPort  — buffer read position (Lua MAX HSET) + pub/sub realtime.
 * ReadPositionBufferPort  — drain buffer để flush job đọc và persist vào DB.
 *
 * Separation of concerns giữa 2 port giúp sau này tách microservice dễ dàng:
 * flush-service chỉ cần inject ReadPositionBufferPort, không cần Pub/Sub logic.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReadReceiptRedisAdapter implements ReadReceiptPublishPort, ReadPositionBufferPort {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /** Hash key: field = userId, value = lastReadSeq */
    private static final String READS_KEY          = "conv:%s:reads";

    /** Set theo dõi conversation nào có data — job dùng thay vì SCAN toàn keyspace */
    private static final String ACTIVE_CONV_SET    = "active:conv:reads";

    private static final long   TTL_SECONDS        = 2 * 24 * 3600; // 48h

    /**
     * Lua script: atomic MAX HSET.
     * Chỉ update nếu seq mới > seq hiện tại → đảm bảo không rollback về seq thấp hơn.
     * EXPIRE reset mỗi lần có activity → TTL sliding window.
     */
    private static final String LUA_MAX_HSET = """
        local current = redis.call('HGET', KEYS[1], ARGV[1])
        if current == false or tonumber(ARGV[2]) > tonumber(current) then
            redis.call('HSET', KEYS[1], ARGV[1], ARGV[2])
            redis.call('EXPIRE', KEYS[1], ARGV[3])
            return 1
        end
        return 0
        """;

    /**
     * Lua script: atomic HGETALL + DEL.
     * Lấy toàn bộ data rồi xóa key trong 1 round-trip.
     *
     * Tại sao xóa ngay thay vì sau khi persist xong:
     *   Nếu xóa SAU: write event mới có thể đến giữa HGETALL và DEL → mất data.
     *   Nếu xóa TRƯỚC (cách này): write mới sau DEL tạo key mới →
     *   flush job lần sau xử lý → không mất data.
     */
    @SuppressWarnings("rawtypes")
    private static final RedisScript<List> HGETALL_DEL = RedisScript.of("""
        local data = redis.call('HGETALL', KEYS[1])
        if #data > 0 then
            redis.call('DEL', KEYS[1])
        end
        return data
        """, List.class);

    // ── ReadReceiptPublishPort ────────────────────────────────────────────────

    @Override
    public void bufferReadReceipt(Long conversationId, Long userId, Long sequenceNumber) {
        String key = String.format(READS_KEY, conversationId);
        redisTemplate.execute(
            RedisScript.of(LUA_MAX_HSET, Long.class),
            List.of(key),
            String.valueOf(userId),
            String.valueOf(sequenceNumber),
            String.valueOf(TTL_SECONDS)
        );
        // Đánh dấu conversation có data để flush job không cần SCAN
        redisTemplate.opsForSet().add(ACTIVE_CONV_SET, conversationId.toString());

        log.debug("Buffered read receipt: conv={}, user={}, seq={}", conversationId, userId, sequenceNumber);
    }

    @Override
    public void publishReadReceipt(Long conversationId, long readerid, Long sequenceNumber, long readAt) {
        String channel = String.format("channel:conv:%d:read-receipts", conversationId);
        ReadReceiptMessage message = new ReadReceiptMessage(
            conversationId.toString(), readerid, sequenceNumber, readAt);
        try {
            redisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(message));
            log.debug("Published read receipt to channel={}", channel);
        } catch (Exception e) {
            log.error("Failed to publish read receipt: {}", e.getMessage(), e);
        }
    }

    // ── ReadPositionBufferPort ────────────────────────────────────────────────

    /**
     * Drain toàn bộ buffer.
     *
     * Flow:
     *   1. Lấy set conversation IDs + xóa set (write mới sẽ tạo set mới)
     *   2. Với mỗi conv: HGETALL + DEL atomic → parse thành map
     *
     * @return conversationId → { userId → lastReadSeq }
     */

    @Override
    public Set<Long> collectActiveConversations() {
        Set<String> activeConvIds = redisTemplate.opsForSet().members(ACTIVE_CONV_SET);
        redisTemplate.delete(ACTIVE_CONV_SET);
        if (activeConvIds == null || activeConvIds.isEmpty()) return Set.of();
        return activeConvIds.stream()
            .map(Long::parseLong)
            .collect(Collectors.toSet());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<Long, Long> getReadPositions(Long conversationId) {
        String key = String.format(READS_KEY, conversationId);
        List<String> entries = (List<String>) redisTemplate.execute(HGETALL_DEL, List.of(key));
        if (entries == null || entries.isEmpty()) return Map.of();

        Map<Long, Long> userSeqMap = new HashMap<>();
        for (int i = 0; i + 1 < entries.size(); i += 2) {
            userSeqMap.put(
                Long.parseLong(entries.get(i)),
                Long.parseLong(entries.get(i + 1)));
        }
        return userSeqMap;
    }

    public record ReadReceiptMessage(
        String conversationId,
        long   readerid,
        Long   sequenceNumber,
        Long   readAt
    ) {}
}
