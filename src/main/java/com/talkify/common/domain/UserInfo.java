package com.talkify.common.domain;

    public record UserInfo(
        long userId,
        String displayName,
        String avatarUrl
    ) {}
