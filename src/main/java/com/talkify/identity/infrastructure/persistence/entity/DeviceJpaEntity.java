package com.talkify.identity.infrastructure.persistence.entity;

import java.time.Instant;

import com.talkify.identity.domain.model.DevicePlatform;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "devices",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_device_token", columnNames = "device_token")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DeviceJpaEntity {

    @Id
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "device_token", nullable = false, length = 512)
    private String deviceToken;

    @Column(name = "platform", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private DevicePlatform platform;

    @Column(name = "device_name", length = 100)
    private String deviceName;

    @Column(name = "last_active_at")
    private Instant lastActiveAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}