package com.talkify.identity.application.service;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.talkify.common.util.Sha256Utils;
import com.talkify.identity.application.dto.SessionResult;
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

    /**
     * Tạo session mới và trả về {@link SessionResult} chứa:
     * <ul>
     *   <li>{@code sessionId} — để đưa vào AT claim "sid".</li>
     *   <li>{@code rawRefreshToken} — để set vào HttpOnly cookie qua controller.</li>
     * </ul>
     *
     * <p>Domain chỉ lưu {@code SHA-256(rawRefreshToken)}, raw token không bao giờ
     * được persist. Caller có trách nhiệm xử lý raw token an toàn (truyền vào
     * {@code AuthResponse} rồi set cookie, không log).
     */
    @Transactional
    public SessionResult createSession(UserId userId, DeviceInfo deviceInfo) {
        Instant now           = clock.instant();
        String rawToken       = jwtPort.generateRefreshToken(userId);
        Instant expiresAt     = now.plusSeconds(jwtPort.getRefreshTokenTtl());

        UserSession session = UserSession.create(
                userId,
                Sha256Utils.hash(rawToken),
                deviceInfo,
                expiresAt,
                now
        );
        sessionRepository.save(session);
        return new SessionResult(session.getId(), rawToken);
    }
}
