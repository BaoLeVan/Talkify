package com.talkify.identity.infrastructure.persistence.adapter;

import com.talkify.identity.domain.model.Email;
import com.talkify.identity.domain.model.User;
import com.talkify.identity.domain.model.Username;
import com.talkify.identity.domain.repository.UserRepository;
import com.talkify.identity.infrastructure.persistence.repository.UserJpaRepository;

public class UserRepositoryAdapter implements UserRepository {
    private final UserJpaRepository userJpaRepository;

    @Override
    public void save(User user) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'save'");
    }

    @Override
    public boolean existsByEmail(Email email) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'existsByEmail'");
    }

    @Override
    public boolean existsByUsername(Username username) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'existsByUsername'");
    }
    
}
