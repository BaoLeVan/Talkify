package com.talkify.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;

/**
 * ShedLock configuration: distributed lock cho @Scheduled jobs dùng Redis.
 *
 * Tại sao Redis thay vì PostgreSQL cho ShedLock:
 *   - Lock là ephemeral data — không cần ACID, không cần persist khi restart.
 *   - Redis SET NX EX là primitive lock tự nhiên, O(1), không block DB connection pool.
 *   - Không cần tạo bảng `shedlock` trong PostgreSQL.
 *   - Cùng Redis instance đã dùng cho buffer read receipt → không thêm infra.
 *
 * Trade-off cần biết:
 *   - Nếu Redis restart hoặc evict key (allkeys-lru), lock mất → tất cả
 *     instance có thể chạy job cùng lúc 1 lần.
 *   - Với ReadMessageFlushJob (idempotent, GREATEST), điều này chấp nhận được:
 *     chỉ tốn thêm 1 DB round-trip, không sai data.
 *   - Nếu cần strict guarantee hơn: dùng Redis với maxmemory-policy=noeviction
 *     hoặc chuyển về JDBC provider.
 *
 * @EnableSchedulerLock(defaultLockAtMostFor = "10m"):
 *   Fallback TTL toàn cục — ngăn deadlock khi instance crash không release lock.
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
public class SchedulerLockConfig {

    /**
     * RedisLockProvider: lưu lock dưới dạng Redis key với TTL = lockAtMostFor.
     * Key pattern: shedlock:{jobName}
     * Dùng cùng RedisConnectionFactory đã auto-configured từ spring.data.redis.*
     */
    @Bean
    public LockProvider lockProvider(RedisConnectionFactory connectionFactory) {
        return new RedisLockProvider(connectionFactory);
    }
}
