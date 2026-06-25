package com.talkify.identity.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.talkify.common.domain.Email;
import com.talkify.common.domain.PhoneNumber;
import com.talkify.common.domain.Username;
import com.talkify.identity.domain.model.User;

public interface UserRepository {
    User save(User user);
    Optional<User> findByEmail(Email email);
    Optional<User> findByUsername(Username username);
    Optional<User> findByPhoneNumber(PhoneNumber phoneNumber);
    Optional<User> findById(Long id);
    List<User> findAllByIds(Collection<Long> ids);

    boolean existsByEmail(Email email);
    boolean existsByUsername(Username username);
    boolean existsByPhoneNumber(PhoneNumber phoneNumber);
}