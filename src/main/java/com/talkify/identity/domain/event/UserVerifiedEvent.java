package com.talkify.identity.domain.event;

import com.talkify.identity.domain.model.UserId;

public record UserVerifiedEvent(UserId userId, String email) {}
