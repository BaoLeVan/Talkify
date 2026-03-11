package com.talkify.identity.domain.repository;

import com.talkify.identity.domain.model.Email;
import com.talkify.identity.domain.model.User;
import com.talkify.identity.domain.model.Username;

public interface UserRepository {
    void save(User user);
    boolean existsByEmail(Email email);
    boolean existsByUsername(Username username);
    
}