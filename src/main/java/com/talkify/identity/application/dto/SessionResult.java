package com.talkify.identity.application.dto;

import java.time.Instant;

import com.talkify.identity.domain.model.SessionId;

public record SessionResult(SessionId sessionId, String rawRefreshToken, Instant expiresAt) {}
