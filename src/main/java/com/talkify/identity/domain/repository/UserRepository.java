package com.talkify.identity.domain.repository;

import java.util.Optional;

import com.talkify.identity.domain.model.Email;
import com.talkify.identity.domain.model.PhoneNumber;
import com.talkify.identity.domain.model.User;
import com.talkify.identity.domain.model.Username;

public interface UserRepository {
    User save(User user);
    Optional<User> findByEmail(Email email);
    Optional<User> findByUsername(Username username);
    Optional<User> findByPhoneNumber(PhoneNumber phoneNumber);
    Optional<User> findById(Long id);
    boolean existsByEmail(Email email);
    boolean existsByUsername(Username username);
    boolean existsByPhoneNumber(PhoneNumber phoneNumber);
}