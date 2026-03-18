package com.talkify.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "security.cookie")
public class CookieProperties {

    @NotBlank
    private String name = "refresh_token";

    @NotBlank
    private String path = "/api/v1/auth";

    @Positive
    private long maxAge = 604800;

    private boolean secure = false;

    private String sameSite = "Strict";
}
