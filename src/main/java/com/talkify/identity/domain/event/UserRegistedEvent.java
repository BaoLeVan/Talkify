package com.talkify.identity.domain.event;

public record UserRegistedEvent(String email, String username, String displayName) {}
