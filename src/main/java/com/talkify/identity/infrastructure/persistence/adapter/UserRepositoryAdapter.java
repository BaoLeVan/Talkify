package com.talkify.identity.infrastructure.persistence.adapter;

import java.util.Optional;

import com.talkify.identity.application.dto.mapper.UserMapper;
import com.talkify.identity.domain.model.Email;
import com.talkify.identity.domain.model.User;
import com.talkify.identity.domain.model.Username;
import com.talkify.identity.domain.repository.UserRepository;
import com.talkify.identity.infrastructure.persistence.entity.UserJpaEntity;
import com.talkify.identity.infrastructure.persistence.repository.UserJpaRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {
    private final UserJpaRepository userJpaRepository;
    private final UserMapper userMapper;

    @Override
    public User save(User user) {
        UserJpaEntity userEntity = userMapper.toEntity(user);
        UserJpaEntity saved = userJpaRepository.save(userEntity);
        return userMapper.toDomain(saved);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return userJpaRepository.existsByEmail(email.value());
    }

    @Override
    public boolean existsByUsername(Username username) {
        return userJpaRepository.existsByUsername(username.value());
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findByEmail'");
    }

    @Override
    public Optional<User> findByUsername(Username username) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findByUsername'");
    }

    @Override
    public Optional<User> findById(Long id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findById'");
    }
    
}
