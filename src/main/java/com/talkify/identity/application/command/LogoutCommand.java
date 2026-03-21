package com.talkify.identity.application.command;

import com.talkify.identity.domain.model.LogoutScope;
import com.talkify.identity.domain.model.SessionId;


public record LogoutCommand(
    SessionId sessionId,
    LogoutScope scope
) {}
