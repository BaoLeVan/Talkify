package com.talkify.identity.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.talkify.identity.infrastructure.persistence.entity.UserJpaEntity;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    
}
