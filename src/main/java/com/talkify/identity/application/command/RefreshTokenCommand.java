package com.talkify.identity.application.command;

/**
 * Command cho refresh-token use case.
 *
 * refreshToken được AuthController trích xuất từ HttpOnly cookie — không từ request body.
 * Do đó không có @NotBlank annotation: validation thực hiện ở controller (kiểm tra cookie exists)
 * và ở SessionHandler (jwtPort.validateToken).
 */
public record RefreshTokenCommand(String refreshToken) {}
