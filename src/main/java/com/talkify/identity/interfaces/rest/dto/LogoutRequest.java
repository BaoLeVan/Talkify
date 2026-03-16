package com.talkify.identity.interfaces.rest.dto;

import com.talkify.identity.domain.model.LogoutScope;

import jakarta.validation.constraints.NotNull;

/**
 * HTTP request body cho POST /logout.
 *
 * Thuộc interface layer — chỉ đại diện cho dữ liệu CLIENT gửi lên.
 * rawRefreshToken KHÔNG nằm ở đây vì nó đến từ HttpOnly cookie,
 * không phải request body.
 *
 * Controller sẽ assemble LogoutCommand (application layer) từ:
 *   - scope (từ request body này)
 *   - rawRefreshToken (từ cookie)
 */
public record LogoutRequest(
    @NotNull LogoutScope scope
) {}
