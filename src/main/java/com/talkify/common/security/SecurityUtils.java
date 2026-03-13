package com.talkify.common.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.talkify.identity.domain.model.UserId;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<UserId> getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserId userId) {
            return Optional.of(userId);
        }
        return Optional.empty();
    }

    public static UserId requireCurrentUserId() {
        return getCurrentUserId()
                .orElseThrow(() -> new IllegalStateException("No authenticated user"));
    }
}