package com.talkify.identity.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.talkify.identity.infrastructure.persistence.entity.UserJpaEntity;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {

    // ── Existence check ───────────────────────────────────────────────────
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByPhoneNumber(String phoneNumber);

    // ── Raw lookup (includes soft-deleted) ─────────────────────────────
    Optional<UserJpaEntity> findByEmail(String email);
    Optional<UserJpaEntity> findByUsername(String username);
    Optional<UserJpaEntity> findByPhoneNumber(String phoneNumber);

    // ── Active-only (excludes soft-deleted rows) ───────────────────────
    @Query("SELECT u FROM UserJpaEntity u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<UserJpaEntity> findActiveByEmail(@Param("email") String email);

    @Query("SELECT u FROM UserJpaEntity u WHERE u.id = :id AND u.deletedAt IS NULL")
    Optional<UserJpaEntity> findActiveById(@Param("id") Long id);

    // ── Soft delete ───────────────────────────────────────────────────
    @Modifying
    @Query("UPDATE UserJpaEntity u SET u.deletedAt = :deletedAt WHERE u.id = :id")
    void softDeleteById(@Param("id") Long id, @Param("deletedAt") java.time.Instant deletedAt);
}
