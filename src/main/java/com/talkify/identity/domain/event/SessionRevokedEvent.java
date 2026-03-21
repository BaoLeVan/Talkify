package com.talkify.identity.domain.event;

import com.talkify.identity.domain.model.SessionId;
import com.talkify.identity.domain.model.UserId;

public record SessionRevokedEvent(SessionId sessionId, UserId userId) {}
