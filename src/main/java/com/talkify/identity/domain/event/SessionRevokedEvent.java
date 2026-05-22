package com.talkify.identity.domain.event;

import com.talkify.common.domain.UserId;
import com.talkify.identity.domain.model.SessionId;

public record SessionRevokedEvent(SessionId sessionId, UserId userId) {}
