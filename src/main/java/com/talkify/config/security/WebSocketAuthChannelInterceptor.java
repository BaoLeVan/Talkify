package com.talkify.config.security;

import java.util.List;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.talkify.common.domain.UserId;
import com.talkify.identity.application.port.JwtPort;
import com.talkify.identity.application.port.SessionCachePort;
import com.talkify.identity.application.port.TokenClaims;
import com.talkify.identity.application.port.TokenParseResult;
import com.talkify.identity.domain.model.SessionId;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtPort          jwtPort;
    private final SessionCachePort sessionCachePort;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        String token = extractBearerToken(accessor);
        if (token == null) {
            log.warn("WS CONNECT rejected: missing Authorization header");
            throw new IllegalArgumentException("Missing Authorization header");
        }

        TokenParseResult parseResult = jwtPort.parseAccessToken(token);

        if (parseResult instanceof TokenParseResult.Expired) {
            log.warn("WS CONNECT rejected: token expired");
            throw new IllegalArgumentException("Token expired");
        }
        if (!(parseResult instanceof TokenParseResult.Valid valid)) {
            log.warn("WS CONNECT rejected: invalid token");
            throw new IllegalArgumentException("Invalid token");
        }

        TokenClaims claims = valid.claims();
        if (!"access".equals(claims.type())) {
            log.warn("WS CONNECT rejected: not an access token");
            throw new IllegalArgumentException("Only access tokens accepted");
        }

        UserId    userId    = UserId.of(Long.parseLong(claims.subject()));
        SessionId sessionId = claims.sessionId();

        if (!sessionCachePort.isSessionValid(sessionId, userId)) {
            log.warn("WS CONNECT rejected: session revoked | userId={}", userId.value());
            throw new IllegalArgumentException("Session revoked");
        }

        accessor.setUser(new UsernamePasswordAuthenticationToken(
                userId.value().toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + claims.role()))
        ));

        log.debug("WS CONNECT authenticated | userId={}", userId.value());
        return message;
    }

    private String extractBearerToken(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        return (header != null && header.startsWith("Bearer ")) ? header.substring(7) : null;
    }
}
