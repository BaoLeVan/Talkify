package com.talkify.common.security;

import com.talkify.common.domain.UserId;
import com.talkify.identity.domain.model.SessionId;

public record AuthPrincipal(UserId userId, SessionId sessionId) {}
