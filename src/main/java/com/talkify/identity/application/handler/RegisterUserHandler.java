package com.talkify.identity.application.handler;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.talkify.common.exception.AppException;
import com.talkify.common.exception.ErrorCode;
import com.talkify.common.id.IdGenerator;
import com.talkify.identity.application.command.RegisterUserCommand;
import com.talkify.identity.application.dto.response.AuthResponse;
import com.talkify.identity.application.dto.response.AuthResponse.UserInfo;
import com.talkify.identity.application.port.JwtPort;
import com.talkify.identity.domain.event.UserRegisteredEvent;
import com.talkify.identity.domain.model.Email;
import com.talkify.identity.domain.model.OtpPurpose;
import com.talkify.identity.domain.model.Password;
import com.talkify.identity.domain.model.User;
import com.talkify.identity.domain.model.UserId;
import com.talkify.identity.domain.model.Username;
import com.talkify.identity.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegisterUserHandler {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final IdGenerator idGenerator;
    private final JwtPort jwtPort;

    @Transactional
    public AuthResponse handle(RegisterUserCommand command) {
        Email email = Email.of(command.email());
        Username username = Username.of(command.username());

        if (userRepository.existsByEmail(email)) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (userRepository.existsByUsername(username)) {
            throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        Password.validateRaw(command.password());
        String hashedPassword = passwordEncoder.encode(command.password());
        UserId userId = UserId.of(idGenerator.nextId());

        User user = User.register(userId, username, email,
                Password.ofHashed(hashedPassword), command.displayName());

        userRepository.save(user);

        String accessToken  = jwtPort.generateAccessToken(user.getId(), user.getRole(), user.getStatus());
        String refreshToken = jwtPort.generateRefreshToken(user.getId());

        eventPublisher.publishEvent(
                new UserRegisteredEvent(email.value(), command.displayName(), OtpPurpose.REGISTRATION)
        );

        return AuthResponse.of(
                accessToken,
                refreshToken,
                new UserInfo(
                        user.getId().value(),
                        user.getEmail().value(),
                        user.getUsername(),
                        null,
                        user.getRole().name(),
                        user.getDisplayName(),
                        user.getStatus().name()
                )
        );
    }
}
