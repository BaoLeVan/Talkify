package com.talkify.common.id;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class SnowflakeIdGenerator implements IdGenerator {
    private static final long EPOCH = 1704067200000L; // 2024-01-01T00:00:00Z tính theo ms
    private static final long WORKER_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;
    private final long workerId;

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator(@Value("${snowflake.worker-id:0}") long workerId) {
        this.workerId = workerId;
    }

    @Override
    public synchronized long nextId() {
        long timestamp = Instant.now().toEpochMilli();

        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards!");
        }

        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & ((1 << SEQUENCE_BITS) - 1);
            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - EPOCH) << (WORKER_ID_BITS + SEQUENCE_BITS))
                | (workerId << SEQUENCE_BITS)
                | sequence;
    }

    private long waitNextMillis(long lastTimestamp) {
        long timestamp = Instant.now().toEpochMilli();
        while (timestamp <= lastTimestamp) {
            timestamp = Instant.now().toEpochMilli();
        }
        return timestamp;
    }
}
