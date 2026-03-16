package com.talkify.identity.application.service;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.talkify.common.util.Sha256Utils;
import com.talkify.identity.application.port.JwtPort;
import com.talkify.identity.domain.model.DeviceInfo;
import com.talkify.identity.domain.model.UserId;
import com.talkify.identity.domain.model.UserSession;
import com.talkify.identity.domain.repository.SessionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final JwtPort           jwtPort;
    private final SessionRepository sessionRepository;
    private final Clock             clock;

    @Transactional
    public String createSession(UserId userId, DeviceInfo deviceInfo) {
        Instant now         = clock.instant();
        String refreshToken = jwtPort.generateRefreshToken(userId);
        Instant expiresAt   = now.plusSeconds(jwtPort.getRefreshTokenTtl());

        UserSession session = UserSession.create(
                userId,
                Sha256Utils.hash(refreshToken),
                deviceInfo,
                expiresAt,
                now
        );
        sessionRepository.save(session);
        return refreshToken;
    }
}
