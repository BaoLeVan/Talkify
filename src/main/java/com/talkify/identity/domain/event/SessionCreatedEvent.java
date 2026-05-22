package com.talkify.identity.domain.event;

import java.time.Instant;

import com.talkify.common.domain.UserId;
import com.talkify.identity.domain.model.SessionId;

public record SessionCreatedEvent(SessionId sessionId, UserId userId, Instant expiresAt) {}
