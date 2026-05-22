package com.talkify.messaging.application.port;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import com.talkify.common.domain.UserInfo;

/**
 * Anti-corruption port — messaging context queries basic user info
 * from the identity context without directly depending on its domain.
 */
public interface UserQueryPort {

    Optional<UserInfo> findById(long userId);

    /**
     * Batch lookup — để tránh N+1 khi list conversations.
     * Trả về Map<userId, UserInfo> cho tất cả user tìm thấy.
     */
    Map<Long, UserInfo> findAllByIds(Collection<Long> userIds);
}
