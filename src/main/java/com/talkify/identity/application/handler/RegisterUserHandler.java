package com.talkify.identity.application.handler;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.talkify.identity.application.command.RegisterUserCommand;
import com.talkify.identity.application.port.EmailPort;
import com.talkify.identity.domain.model.Email;
import com.talkify.identity.domain.model.Username;
import com.talkify.identity.domain.model.Password;
import com.talkify.identity.domain.model.User;
import com.talkify.identity.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegisterUserHandler {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailPort emailPort;
    private final ApplicationEventPublisher eventPublisher;
    
    @Transactional
    public void handle(RegisterUserCommand command) {
        Email email = Email.of(command.email());
        Username username = Username.of(command.username());

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already in use");
        }

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already in use");
        }

        Password.validateRaw(command.password());
        String hasedPassword = passwordEncoder.encode(command.password());

        User user = User.register(username, email, Password.ofHashed(hasedPassword), command.displayName());

        userRepository.save(user);

        user.pullDomainEvents().forEach(eventPublisher::publishEvent);
    }
    
}
