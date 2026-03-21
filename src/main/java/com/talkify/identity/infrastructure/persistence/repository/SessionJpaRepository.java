package com.talkify.identity.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.talkify.identity.infrastructure.persistence.entity.SessionJpaEntity;

public interface SessionJpaRepository extends JpaRepository<SessionJpaEntity, Long> {

    Optional<SessionJpaEntity> findByTokenHash(String tokenHash);

    List<SessionJpaEntity> findByUserIdAndRevokedAtIsNull(Long userId);

    @Modifying
    @Query("UPDATE SessionJpaEntity s SET s.revokedAt = :now " +
           "WHERE s.id = :id AND s.revokedAt IS NULL")
    void revokeById(@Param("id") Long id, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE SessionJpaEntity s SET s.revokedAt = :now " +
           "WHERE s.tokenHash = :tokenHash AND s.revokedAt IS NULL")
    void revokeByTokenHash(@Param("tokenHash") String tokenHash,
                           @Param("now") Instant now);

    @Modifying
    @Query("UPDATE SessionJpaEntity s SET s.revokedAt = :now " +
           "WHERE s.userId = :userId AND s.id IN :ids")
    void revokeByUserIdAndIdIn(@Param("userId") Long userId,
                               @Param("ids") Collection<Long> ids,
                               @Param("now") Instant now);

    @Modifying
    @Query("UPDATE SessionJpaEntity s SET s.revokedAt = :now " +
           "WHERE s.userId = :userId AND s.revokedAt IS NULL")
    void revokeAllByUserId(@Param("userId") Long userId,
                           @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM SessionJpaEntity s WHERE s.expiresAt < :now")
    void deleteExpiredBefore(@Param("now") Instant now);

    @Modifying
    @Query("UPDATE SessionJpaEntity s SET s.revokedAt = :now " +
           "WHERE s.userId = :userId AND s.id <> :sessionId AND s.revokedAt IS NULL")
    void revokeAllByUserIdExceptSessionId(@Param("userId") Long userId,
                                         @Param("sessionId") Long sessionId,
                                         @Param("now") Instant now);
}
