package com.talkify.common.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.talkify.identity.domain.model.SessionId;
import com.talkify.identity.domain.model.UserId;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<UserId> getCurrentUserId() {
        return getPrincipal().map(AuthPrincipal::userId);
    }

    public static UserId requireCurrentUserId() {
        return getCurrentUserId()
                .orElseThrow(() -> new IllegalStateException("No authenticated user"));
    }

    public static Optional<SessionId> getCurrentSessionId() {
        return getPrincipal().map(AuthPrincipal::sessionId);
    }

    public static SessionId requireCurrentSessionId() {
        return getCurrentSessionId()
                .orElseThrow(() -> new IllegalStateException("No session ID in security context"));
    }

    private static Optional<AuthPrincipal> getPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }
}