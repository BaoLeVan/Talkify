package com.talkify.messaging.domain.port;

/**
 * Domain port — Atomic sequence number generation per conversation.
 * 
 * WHY this is a domain port (not application):
 * - Sequence numbers are a DOMAIN INVARIANT: messages within a conversation 
 *   MUST have unique, monotonically increasing sequence numbers.
 * - The domain defines WHAT it needs; infrastructure decides HOW (Redis INCR, DB sequence, etc.)
 * 
 * Contract:
 * - MUST be atomic (concurrent sends must not produce duplicates)
 * - MUST be monotonically increasing for a given conversationId
 * - Gap-free is NOT required (acceptable to skip numbers on failures)
 * 
 * Adapter: RedisSequenceGenerator (uses Redis INCR for atomic increment)
 * Future: PostgreSQL sequence / DynamoDB atomic counter
 */
public interface SequenceGenerator {

    /**
     * Atomically allocate the next sequence number for the given conversation.
     * Thread-safe, distributed-safe.
     *
     * @param conversationId the conversation's unique identifier
     * @return the next sequence number (starts at 1)
     */
    long nextSequence(long conversationId);
}
