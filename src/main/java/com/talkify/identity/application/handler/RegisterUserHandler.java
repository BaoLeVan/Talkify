package com.talkify.identity.application.handler;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.talkify.common.exception.AppException;
import com.talkify.common.exception.ErrorCode;
import com.talkify.common.id.IdGenerator;
import com.talkify.identity.application.command.RegisterUserCommand;
import com.talkify.identity.domain.model.Email;
import com.talkify.identity.domain.model.Username;
import com.talkify.identity.domain.model.Password;
import com.talkify.identity.domain.model.User;
import com.talkify.identity.domain.model.UserId;
import com.talkify.identity.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegisterUserHandler {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    // private final EmailPort emailPort;
    private final ApplicationEventPublisher eventPublisher;
    private final IdGenerator idGenerator;
    
    @Transactional
    public void handle(RegisterUserCommand command) {
        Email email = Email.of(command.email());
        Username username = Username.of(command.username());

        if (userRepository.existsByEmail(email)) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if (userRepository.existsByUsername(username)) {
            throw new AppException(ErrorCode.USER_NAME_ALREADY_EXISTS);
        }

        Password.validateRaw(command.password());
        String hasedPassword = passwordEncoder.encode(command.password());
        UserId userId = UserId.of(idGenerator.nextId());

        User user = User.register(userId, username, email, Password.ofHashed(hasedPassword), command.displayName());

        userRepository.save(user);

        user.pullDomainEvents().forEach(eventPublisher::publishEvent);
    }
    
}
