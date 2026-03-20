package com.talkify.identity.application.port;

import com.talkify.identity.domain.model.SessionId;

public record TokenClaims(
        String subject,
        String type,
        String role,
        String status,
        SessionId sessionId
) {}
