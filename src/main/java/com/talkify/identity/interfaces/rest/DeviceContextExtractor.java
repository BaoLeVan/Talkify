package com.talkify.identity.interfaces.rest;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.talkify.identity.domain.model.DeviceInfo;
import com.talkify.identity.domain.model.DevicePlatform;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class DeviceContextExtractor {

    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_X_REAL_IP       = "X-Real-IP";

    public DeviceInfo extract(HttpServletRequest request) {
        String userAgent = Optional.ofNullable(request.getHeader("User-Agent")).orElse("");
        String ip        = extractClientIp(request);
        DevicePlatform platform = detectPlatform(userAgent);
        String deviceName       = buildDeviceName(userAgent, platform);
        return DeviceInfo.of(deviceName, platform, ip);
    }

    // ── IP extraction ──────────────────────────────────────────────────────

    private String extractClientIp(HttpServletRequest request) {
        // X-Forwarded-For có thể chứa nhiều IP: "client, proxy1, proxy2"
        String xff = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String xRealIp = request.getHeader(HEADER_X_REAL_IP);
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }

    // ── Platform detection ─────────────────────────────────────────────────

    private DevicePlatform detectPlatform(String ua) {
        if (ua.contains("Android"))                                          return DevicePlatform.ANDROID;
        if (ua.contains("iPhone") || ua.contains("iPad") || ua.contains("iPod")) return DevicePlatform.IOS;
        return DevicePlatform.WEB;
    }

    // ── Device name building ───────────────────────────────────────────────

    private String buildDeviceName(String ua, DevicePlatform platform) {
        if (ua.isBlank()) return "Unknown Device";
        String browser = detectBrowser(ua);
        String os = switch (platform) {
            case ANDROID -> "Android";
            case IOS     -> "iOS";
            case WEB     -> detectDesktopOs(ua);
        };
        return browser + " on " + os;
    }

    /**
     * Order matters: Edge/Opera/CriOS must be checked before Chrome/Safari.
     * TODO: Consider replacing this manual parsing with ua-parser (https://github.com/ua-parser/uap-java)
     *       for broader coverage (Brave, Samsung Internet, Vivaldi, etc.) before going to production.
     */
    private String detectBrowser(String ua) {
        if (ua.contains("Edg/") || ua.contains("EdgA/")) return "Edge";
        if (ua.contains("OPR/") || ua.contains("Opera/"))  return "Opera";
        if (ua.contains("CriOS/"))                          return "Chrome";   // Chrome on iOS
        if (ua.contains("Chrome/"))                         return "Chrome";
        if (ua.contains("FxiOS/") || ua.contains("Firefox/")) return "Firefox";
        if (ua.contains("Safari/"))                         return "Safari";
        return "Unknown Browser";
    }

    private String detectDesktopOs(String ua) {
        if (ua.contains("Windows"))  return "Windows";
        if (ua.contains("Mac OS X")) return "macOS";
        if (ua.contains("Linux"))    return "Linux";
        return "Unknown OS";
    }
}
