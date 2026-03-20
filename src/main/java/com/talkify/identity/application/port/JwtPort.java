package com.talkify.identity.application.port;

import com.talkify.identity.domain.model.SessionId;
import com.talkify.identity.domain.model.UserId;
import com.talkify.identity.domain.model.UserRole;
import com.talkify.identity.domain.model.UserStatus;

public interface JwtPort {
    String generateAccessToken(UserId userId, SessionId sessionId, UserRole role, UserStatus status);
    String generateRefreshToken(UserId userId);
    boolean validateToken(String token);
    UserId extractUserId(String token);
    TokenClaims extractAllClaims(String token);
    /** TTL của refresh token tính bằng giây — dùng để tính expiresAt khi tạo session. */
    long getRefreshTokenTtl();
    /**
     * Ngưỡng gần hết hạn tính bằng giây (default 1 ngày).
     * Nếu remaining TTL ≤ threshold → proactive rotation.
     */
    long getRefreshThreshold();
}
