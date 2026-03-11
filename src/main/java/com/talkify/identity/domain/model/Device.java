package com.talkify.identity.domain.model;

import java.time.Instant;
import java.util.Objects;

public class Device {
    private Long id;
    private String deviceFingerprint;
    private String deviceToken;
    private DevicePlatform platform;
    private String deviceName;
    private Instant lastActiveAt;
    private Instant createdAt;

    private Device() {}

    public static Device register(
            String deviceFingerprint, 
            String deviceName,
            String deviceToken, 
            DevicePlatform platform
        ) {
        if (deviceFingerprint == null || deviceFingerprint.isEmpty()) {
            throw new IllegalArgumentException("Device fingerprint cannot be null or empty");
        }
        
        Device device = new Device();
        device.deviceFingerprint = deviceFingerprint;
        device.deviceToken = deviceToken;
        device.platform = platform;
        device.deviceName = deviceName;
        device.lastActiveAt = Instant.now();
        device.createdAt = Instant.now();
        return device;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getDeviceFingerprint() {
        return deviceFingerprint;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public DevicePlatform getPlatform() {
        return platform;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public Instant getLastActiveAt() {
        return lastActiveAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void touch() {
        this.lastActiveAt = Instant.now();
    }

    public void updateToken(String deviceToken) {
        this.deviceToken = deviceToken;
        this.lastActiveAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Device)) return false;
        return Objects.equals(deviceFingerprint, ((Device) o).deviceFingerprint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceFingerprint);
    }
}