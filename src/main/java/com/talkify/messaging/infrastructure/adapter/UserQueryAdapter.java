package com.talkify.messaging.infrastructure.adapter;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.talkify.common.domain.UserInfo;
import com.talkify.identity.domain.repository.UserRepository;
import com.talkify.messaging.application.port.UserQueryPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserQueryAdapter implements UserQueryPort {

    private final UserRepository userRepository;

    @Override
    public Optional<UserInfo> findById(long userId) {
        return userRepository.findById(userId)
                .map(u -> new UserInfo(u.getId().value(), u.getDisplayName(), u.getAvatarUrl()));
    }

    @Override
    public Map<Long, UserInfo> findAllByIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Map.of();
        return userRepository.findAllByIds(userIds).stream()
                .map(u -> new UserInfo(u.getId().value(), u.getDisplayName(), u.getAvatarUrl()))
                .collect(Collectors.toMap(UserInfo::userId, info -> info));
    }
}
