package com.talkify.identity.application.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthResponse(
    @JsonProperty("access_token") String accessToken,
    /**
     * Refresh token — KHÔNG bao giờ sửa trong JSON response (luôn null khi trả về client).
     * RT được truyền qua HttpOnly cookie bởi AuthController.
     * Field này chỉ tồn tại như communication channel trong application layer
     * (handler → controller), không phải phần của HTTP response body.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("refresh_token")
    String refreshToken,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("user") UserInfo user
) {
    public record UserInfo(
        Long id,
        String email,
        String username,
        String phoneNumber,
        String role,
        @JsonProperty("display_name") String displayName,
        String status
    ) {}

    public static AuthResponse of(String accessToken, String refreshToken, UserInfo user) {
        return new AuthResponse(accessToken, refreshToken, user);
    }

    /**
     * Tạo AuthResponse không có refresh token — dùng khi trả về HTTP response.
     * RT được xử lý riêng qua cookie, không nằm trong body.
     */
    public static AuthResponse withoutToken(AuthResponse source) {
        return new AuthResponse(source.accessToken(), null, source.user());
    }
}
