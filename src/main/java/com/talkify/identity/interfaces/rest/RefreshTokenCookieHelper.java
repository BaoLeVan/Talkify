package com.talkify.identity.interfaces.rest;

import java.util.Arrays;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.talkify.config.security.CookieProperties;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Interface-layer helper: đọc / ghi Refresh Token qua HttpOnly cookie.
 *
 * TẠI SAO CẦN LỚP NÀY (thay vì dùng jakarta.servlet.Cookie trực tiếp)?
 * → jakarta.servlet.Cookie không hỗ trợ thuộc tính SameSite.
 *   Spring's ResponseCookie (dùng Set-Cookie header thô) mới set được đầy đủ attributes.
 *
 * Cookie attributes được set:
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  HttpOnly=true   → JS không đọc được → chặn XSS steal RT           │
 * │  Secure=true     → chỉ gửi qua HTTPS (production)                  │
 * │  SameSite=Strict → cookie không gửi trong cross-site request → CSRF │
 * │  Path=/api/v1/auth → không gửi đến /api/v1/* khác → giảm exposure  │
 * │  Max-Age=604800  → sống 7 ngày, khớp với RT TTL                     │
 * └─────────────────────────────────────────────────────────────────────┘
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenCookieHelper {

    private final CookieProperties cookieProperties;

    public void setRefreshTokenCookie(HttpServletResponse response, String rawToken) {
        ResponseCookie cookie = ResponseCookie.from(cookieProperties.getName(), rawToken)
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .path(cookieProperties.getPath())
                .maxAge(cookieProperties.getMaxAge())
                .sameSite(cookieProperties.getSameSite())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie expired = ResponseCookie.from(cookieProperties.getName(), "")
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .path(cookieProperties.getPath())
                .maxAge(0)
                .sameSite(cookieProperties.getSameSite())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, expired.toString());
    }

    public Optional<String> extractRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        return Arrays.stream(cookies)
                .filter(c -> cookieProperties.getName().equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> v != null && !v.isBlank())
                .findFirst();
    }
}
