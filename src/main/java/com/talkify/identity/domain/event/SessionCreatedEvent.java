package com.talkify.identity.domain.event;

import java.time.Instant;

import com.talkify.identity.domain.model.SessionId;
import com.talkify.identity.domain.model.UserId;

public record SessionCreatedEvent(SessionId sessionId, UserId userId, Instant expiresAt) {}
