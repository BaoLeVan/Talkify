package com.talkify.messaging.application.port;

/**
 * Generic cache port for application-level caching needs.
 * 
 * NOTE: Sequence number generation has been extracted to the domain port
 * {@link com.talkify.messaging.domain.port.SequenceGenerator} where it
 * semantically belongs (it's a domain invariant, not a cache concern).
 * 
 * This port remains for other caching needs:
 * - User online status caching
 * - Rate limiting counters
 * - Session data
 * 
 * @deprecated for sequence numbers. Use {@code SequenceGenerator} instead.
 */
public interface CachePort {

    /**
     * @deprecated Use {@link com.talkify.messaging.domain.port.SequenceGenerator#nextSequence(long)}
     */
    @Deprecated(forRemoval = true)
    long getNextSequenceNumber(Long conversationId);
}
