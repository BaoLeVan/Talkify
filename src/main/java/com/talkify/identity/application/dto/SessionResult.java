package com.talkify.identity.application.dto;

import com.talkify.identity.domain.model.SessionId;

/**
 * Application-layer result của {@code SessionService.createSession()}.
 *
 * <p>Tách biệt rõ ràng giữa:
 * <ul>
 *   <li>{@code sessionId} — đưa vào AT claim "sid" để enable instant revocation.</li>
 *   <li>{@code rawRefreshToken} — gửi qua HttpOnly cookie, KHÔNG lưu trong domain.
 *       Domain chỉ lưu {@code SHA-256(rawRefreshToken)}.</li>
 * </ul>
 *
 * <p>Không phải domain object, không phải HTTP response DTO —
 * chỉ là carrier object giữa {@code SessionService} và các handler trong application layer.
 */
public record SessionResult(SessionId sessionId, String rawRefreshToken) {}
