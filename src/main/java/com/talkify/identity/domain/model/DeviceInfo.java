package com.talkify.identity.domain.model;

import java.util.Objects;

/**
 * Value Object — metadata của thiết bị tạo ra một session.
 *
 * Immutable: không thay đổi trong suốt vòng đời của UserSession.
 * Nếu user login lại từ cùng thiết bị → tạo UserSession mới với DeviceInfo mới.
 *
 * Phân biệt với Device (push notification):
 *   DeviceInfo: "session này được tạo từ thiết bị gì / IP nào"   (display info)
 *   Device:     "push notification gửi đến FCM/APNs token nào"   (operational)
 *
 * Maps to: các cột device_name, device_type, ip_address trong bảng refresh_tokens.
 */
public record DeviceInfo(
        String deviceName,      // "Chrome on MacOS", "iPhone 15", "Samsung Galaxy S24"
        DevicePlatform platform, // WEB / IOS / ANDROID
        String ipAddress        // IPv4 hoặc IPv6 lúc đăng nhập
) {

    public DeviceInfo {
        // deviceName và ipAddress có thể null (client không bắt buộc gửi)
        // platform không được null — phải biết loại thiết bị
        Objects.requireNonNull(platform, "DeviceInfo.platform cannot be null");
    }

    /**
     * Factory — tạo DeviceInfo với đầy đủ thông tin (từ client gửi lên).
     */
    public static DeviceInfo of(String deviceName, DevicePlatform platform, String ipAddress) {
        return new DeviceInfo(deviceName, platform, ipAddress);
    }

    /**
     * Factory — tạo DeviceInfo tối thiểu khi client không gửi device name.
     */
    public static DeviceInfo ofUnknown(DevicePlatform platform, String ipAddress) {
        return new DeviceInfo("Unknown Device", platform, ipAddress);
    }
}
