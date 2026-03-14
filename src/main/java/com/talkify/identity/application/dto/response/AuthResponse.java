package com.talkify.identity.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("refresh_token") String refreshToken,
    @JsonProperty("user") UserInfo user
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
}
