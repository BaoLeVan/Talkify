package com.talkify.identity.domain.model;

import java.time.Instant;
import java.util.Objects;

public class Device {
    private Long id;
    private String deviceFingerprint;
    private String deviceToken;
    private String platform;
    private String deviceName;
    private Instant lastActiveAt;
    private Instant createdAt;

    private Device() {}

    public static Device register(
            String deviceFingerprint, 
            String deviceName,
            String deviceToken, 
            String platform
        ) {
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