package com.talkify.identity.application.port;

public record TokenClaims(
        String subject,
        String type,
        String email,
        String role,
        String status
) {}
