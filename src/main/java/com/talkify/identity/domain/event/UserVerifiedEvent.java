package com.talkify.identity.domain.event;

import com.talkify.common.domain.UserId;

public record UserVerifiedEvent(UserId userId, String email) {}
