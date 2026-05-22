package com.talkify.messaging.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.talkify.messaging.infrastructure.persistence.entity.ConversationJpaEntity;

public interface ConversationJpaRepository extends JpaRepository<ConversationJpaEntity, Long> {

    Optional<ConversationJpaEntity> findById(Long id);

    /**
     * Fetch conversation with participants in a single JOIN FETCH query.
     * Use this whenever domain logic needs to access participants (e.g. assertMember).
     */
    @Query("""
        SELECT c FROM ConversationJpaEntity c
        LEFT JOIN FETCH c.participants
        WHERE c.id = :id
        """)
    Optional<ConversationJpaEntity> findByIdWithParticipants(@Param("id") Long id);

    // First page: no cursor, just sort + limit
    @Query("""
        SELECT c FROM ConversationJpaEntity c
        JOIN c.participants p
        WHERE p.userId = :userId
          AND p.leftAt IS NULL
        ORDER BY c.lastMessageAt DESC NULLS LAST, c.id DESC
        """)
    List<ConversationJpaEntity> findByUserIdFirstPage(
        @Param("userId") Long userId,
        Pageable pageable
    );

    // Subsequent pages: (lastMessageAt, id) < cursor  (keyset pagination)
    @Query("""
        SELECT c FROM ConversationJpaEntity c
        JOIN c.participants p
        WHERE p.userId = :userId
          AND p.leftAt IS NULL
          AND (
            c.lastMessageAt < :cursorTs
            OR (c.lastMessageAt = :cursorTs AND c.id < :cursorId)
          )
        ORDER BY c.lastMessageAt DESC NULLS LAST, c.id DESC
        """)
    List<ConversationJpaEntity> findByUserIdAfterCursor(
        @Param("userId")   Long userId,
        @Param("cursorTs") Instant cursorTs,
        @Param("cursorId") Long cursorId,
        Pageable pageable
    );

    // Check DIRECT conversation between 2 users
    @Query("""
        SELECT c FROM ConversationJpaEntity c
        JOIN c.participants p1 ON p1.userId = :userId1 AND p1.leftAt IS NULL
        JOIN c.participants p2 ON p2.userId = :userId2 AND p2.leftAt IS NULL
        WHERE c.type = 'DIRECT'
        """)
    Optional<ConversationJpaEntity> findDirectBetween(
        @Param("userId1") Long userId1,
        @Param("userId2") Long userId2
    );
}

