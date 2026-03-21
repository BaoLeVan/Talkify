package com.talkify.identity.application.handler;

import java.time.Duration;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.talkify.common.exception.AppException;
import com.talkify.common.exception.ErrorCode;
import com.talkify.identity.application.command.LoginCommand;
import com.talkify.identity.application.command.SendOtpCommand;
import com.talkify.identity.application.dto.SessionResult;
import com.talkify.identity.application.dto.response.AuthResponse;
import com.talkify.identity.application.dto.response.AuthResponse.UserInfo;
import com.talkify.identity.application.port.JwtPort;
import com.talkify.identity.application.service.SessionService;
import com.talkify.identity.domain.model.DeviceInfo;
import com.talkify.identity.domain.model.Email;
import com.talkify.identity.domain.model.Identifier;
import com.talkify.identity.domain.model.OtpPurpose;
import com.talkify.identity.domain.model.PhoneNumber;
import com.talkify.identity.domain.model.User;
import com.talkify.identity.domain.model.UserStatus;
import com.talkify.identity.domain.model.Username;
import com.talkify.identity.domain.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginHandler {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpHandler otpHandler;
    private final SessionService sessionService;
    private final JwtPort jwtPort;

    @Transactional
    public AuthResponse handle(LoginCommand command, DeviceInfo deviceInfo) {
        Identifier identifier = Identifier.of(command.identifier());

        User user = findUserByIdentifier(identifier)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(command.password(), user.getPassword().value())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        return switch (user.getStatus()) {
            case ACTIVE, INACTIVE -> {
                if (user.getStatus() == UserStatus.INACTIVE) {
                    otpHandler.handle(new SendOtpCommand(user.getEmail().value(), OtpPurpose.REGISTRATION));
                }

                SessionResult sessionResult = sessionService.createSession(user.getId(), deviceInfo);
                String accessToken = jwtPort.issueAccessToken(
                        user.getId(),
                        sessionResult.sessionId(),
                        user.getRole(),
                        user.getStatus()
                );

                yield AuthResponse.of(
                        accessToken,
                        sessionResult.rawRefreshToken(),
                        new UserInfo(
                                user.getId().value(),
                                user.getEmail().value(),
                                user.getUsername(),
                                maskPhone(user.getPhoneNumber()),
                                user.getRole().name(),
                                user.getDisplayName(),
                                user.getStatus().name()
                        )
                );
            }

            case BANNED  -> throw new AppException(ErrorCode.USER_BANNED);

            case DELETED -> throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        };
    }

    private Optional<User> findUserByIdentifier(Identifier identifier) {
        return switch (identifier.type()) {
            case EMAIL        -> userRepository.findByEmail(new Email(identifier.value()));
            case PHONE_NUMBER -> userRepository.findByPhoneNumber(new PhoneNumber(identifier.value()));
            case USERNAME     -> userRepository.findByUsername(new Username(identifier.value()));
        };
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() <= 4) return phone;
        int visibleSuffix = 2;
        int visiblePrefix = Math.min(4, phone.length() - visibleSuffix);
        return phone.substring(0, visiblePrefix)
                + "*".repeat(phone.length() - visiblePrefix - visibleSuffix)
                + phone.substring(phone.length() - visibleSuffix);
    }
}
