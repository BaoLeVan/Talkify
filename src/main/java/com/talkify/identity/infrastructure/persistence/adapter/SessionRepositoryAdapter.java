package com.talkify.identity.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.talkify.common.id.IdGenerator;
import com.talkify.identity.domain.model.DeviceInfo;
import com.talkify.identity.domain.model.SessionId;
import com.talkify.identity.domain.model.UserId;
import com.talkify.identity.domain.model.UserSession;
import com.talkify.identity.domain.repository.SessionRepository;
import com.talkify.identity.infrastructure.persistence.entity.SessionJpaEntity;
import com.talkify.identity.infrastructure.persistence.repository.SessionJpaRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SessionRepositoryAdapter implements SessionRepository {

    private final SessionJpaRepository sessionJpaRepository;
    private final IdGenerator idGenerator;

    @Override
    @Transactional
    public UserSession save(UserSession session) {
        // Generate ID for new sessions that don't have one yet
        if (session.getId() == null) {
            session.assignId(SessionId.of(idGenerator.nextId()));
        }
        SessionJpaEntity entity = toEntity(session);
        SessionJpaEntity saved = sessionJpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<UserSession> findByTokenHash(String tokenHash) {
        return sessionJpaRepository.findByTokenHash(tokenHash)
                .map(this::toDomain);
    }

    @Override
    public List<UserSession> findAllActiveByUserId(UserId userId) {
        return sessionJpaRepository.findByUserIdAndRevokedAtIsNull(userId.value())
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void revokeByTokenHash(String tokenHash) {
        sessionJpaRepository.revokeByTokenHash(tokenHash, Instant.now());
    }

    @Override
    @Transactional
    public void revokeByIds(UserId userId, List<SessionId> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return;
        }
        List<Long> ids = sessionIds.stream()
                .map(SessionId::value)
                .toList();
        sessionJpaRepository.revokeByUserIdAndIdIn(userId.value(), ids, Instant.now());
    }

    @Override
    @Transactional
    public void revokeAllByUserId(UserId userId) {
        sessionJpaRepository.revokeAllByUserId(userId.value(), Instant.now());
    }

    @Override
    @Transactional
    public void deleteExpired() {
        sessionJpaRepository.deleteExpiredBefore(Instant.now());
    }

    // ── Mapping helpers ────────────────────────────────────────────────────

    private SessionJpaEntity toEntity(UserSession session) {
        DeviceInfo di = session.getDeviceInfo();
        return SessionJpaEntity.builder()
                .id(session.getId().value())
                .userId(session.getUserId().value())
                .tokenHash(session.getTokenHash())
                .deviceName(di.deviceName())
                .deviceType(di.platform())
                .ipAddress(di.ipAddress())
                .device(null)   // push-notification Device FK is managed separately
                .expiresAt(session.getExpiresAt())
                .lastUsedAt(session.getLastUsedAt())
                .revokedAt(session.getRevokedAt())
                .createdAt(session.getCreatedAt())
                .build();
    }

    private UserSession toDomain(SessionJpaEntity entity) {
        DeviceInfo di = DeviceInfo.of(
                entity.getDeviceName(),
                entity.getDeviceType(),
                entity.getIpAddress());
        return UserSession.reconstruct(
                SessionId.of(entity.getId()),
                UserId.of(entity.getUserId()),
                entity.getTokenHash(),
                di,
                entity.getExpiresAt(),
                entity.getLastUsedAt(),
                entity.getCreatedAt(),
                entity.getRevokedAt());
    }
}
