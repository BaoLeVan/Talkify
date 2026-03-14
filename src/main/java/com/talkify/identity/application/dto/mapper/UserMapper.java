package com.talkify.identity.application.dto.mapper;

import org.springframework.stereotype.Component;

import com.talkify.identity.domain.model.Email;
import com.talkify.identity.domain.model.Password;
import com.talkify.identity.domain.model.User;
import com.talkify.identity.domain.model.UserId;
import com.talkify.identity.domain.model.UserRole;
import com.talkify.identity.domain.model.UserStatus;
import com.talkify.identity.infrastructure.persistence.entity.UserJpaEntity;

@Component
public class UserMapper {
    public UserJpaEntity toEntity(User user) {
        if (user == null) {
            return null;
        }
        
        return UserJpaEntity.builder()
                .id(user.getId().value())
                .email(user.getEmail().value())
                .username(user.getUsername())
                .password(user.getPassword().value())
                .displayName(user.getDisplayName())
                .role(user.getRole() != null ? user.getRole() : UserRole.USER)
                .status(user.getStatus() != null ? user.getStatus() : UserStatus.INACTIVE)
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .deletedAt(user.getDeletedAt())
                .build();
    }

    public User toDomain(UserJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return User.reconstruct(
                UserId.of(entity.getId()),
                entity.getUsername(),
                entity.getDisplayName(),
                Email.of(entity.getEmail()),
                entity.getPhoneNumber(),
                Password.ofHashed(entity.getPassword()),
                entity.getRole(),
                entity.getStatus(),
                entity.getDeletedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
