package com.talkify.config.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.talkify.common.domain.UserId;
import com.talkify.common.exception.ErrorCode;
import com.talkify.dto.response.ApiResponse;
import com.talkify.common.security.AuthPrincipal;
import com.talkify.identity.application.port.JwtPort;
import com.talkify.identity.application.port.SessionCachePort;
import com.talkify.identity.application.port.TokenClaims;
import com.talkify.identity.application.port.TokenParseResult;
import com.talkify.identity.domain.model.SessionId;
import com.talkify.identity.domain.model.UserStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER      = "Authorization";
    private static final String BEARER_PREFIX    = "Bearer ";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final String[] SKIP_FILTER_PATHS = {
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh-token",
    };

    private static final String[] ALLOW_PATHS_FOR_INACTIVE_USER = {
            "/api/v1/auth/send-otp",
            "/api/v1/auth/verify-otp",
            "/api/v1/auth/resend-otp",
    };

    private final JwtPort      jwtPort;
    private final SessionCachePort sessionCachePort;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        for (String skipPath : SKIP_FILTER_PATHS) {
            if (PATH_MATCHER.match(skipPath, path)) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = extractToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        TokenParseResult parseResult = jwtPort.parseAccessToken(token);
        if (parseResult instanceof TokenParseResult.Expired) {
            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.TOKEN_EXPIRED);
            return;
        }
        if (!(parseResult instanceof TokenParseResult.Valid valid)) {
            filterChain.doFilter(request, response);
            return;
        }

        TokenClaims claims = valid.claims();

        if (!"access".equals(claims.type())) {
            log.warn("Rejected non-access token | type={} path={}",
                    claims.type(), request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        UserId    userId    = UserId.of(Long.parseLong(claims.subject()));
        SessionId sessionId = claims.sessionId();

        if (!sessionCachePort.isSessionValid(sessionId, userId)) {
            log.warn("Session not found or revoked | userId={} sessionId={}",
                    userId.value(), sessionId.value());
            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.SESSION_NOT_FOUND);
            return;
        }

        String status = claims.status();
        if (UserStatus.BANNED.name().equals(status)) {
            log.warn("Banned user attempt | userId={}", userId.value());
            writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, ErrorCode.USER_BANNED);
            return;
        }
        if (UserStatus.DELETED.name().equals(status)) {
            log.warn("Deleted user attempt | userId={}", userId.value());
            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.INVALID_CREDENTIALS);
            return;
        }
        if (UserStatus.INACTIVE.name().equals(status)
                && List.of(ALLOW_PATHS_FOR_INACTIVE_USER).stream()
                        .noneMatch(pattern -> PATH_MATCHER.match(pattern, request.getServletPath()))) {
            log.warn("INACTIVE user blocked | userId={} path={}",
                    userId.value(), request.getRequestURI());
            writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, ErrorCode.USER_NOT_VERIFIED);
            return;
        }
        // ─────────────────────────────────────────────────────────────────────

        try {
            var auth = new UsernamePasswordAuthenticationToken(
                    new AuthPrincipal(userId, sessionId),
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + claims.role()))
            );
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (Exception e) {
            log.warn("Cannot set authentication: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private void writeErrorResponse(HttpServletResponse response, int status, ErrorCode errorCode) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.error(errorCode));
    }
}
