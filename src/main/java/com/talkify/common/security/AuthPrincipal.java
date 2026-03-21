package com.talkify.common.security;

import com.talkify.identity.domain.model.SessionId;
import com.talkify.identity.domain.model.UserId;

public record AuthPrincipal(UserId userId, SessionId sessionId) {}
