package com.talkify.identity.application.command;

import com.talkify.identity.domain.model.LogoutScope;

/**
 * Application-layer command cho logout use case.
 *
 * rawRefreshToken: trích xuất từ HttpOnly cookie bởi controller. Nullable.
 * scope: xác định phạm vi thu hồi session.
 *
 * Không có Jakarta Validation annotation — đây là internal object,
 * không được deserialize trực tiếp từ HTTP request body.
 * Validation xảy ra ở interface layer (LogoutRequest).
 */
public record LogoutCommand(
    String rawRefreshToken,
    LogoutScope scope
) {}
