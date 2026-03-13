package com.talkify.config.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.talkify.common.exception.ErrorCode;
import com.talkify.dto.response.ApiResponse;
import com.talkify.identity.application.port.JwtPort;
import com.talkify.identity.application.port.TokenClaims;
import com.talkify.identity.domain.model.UserId;
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

    /**
     * INACTIVE user được phép gọi các endpoint auth (đã là public, không cần JWT),
     * nhưng nếu họ đính kèm JWT vào bất kỳ path nào ngoài prefix này → USER_NOT_VERIFIED.
     */
    private static final String[] ALLOW_PATHS_FOR_INACTIVE_USER = {
            "/api/v1/auth/send-otp",
            "/api/v1/auth/verify-otp",
    };

    private final JwtPort      jwtPort;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null && jwtPort.validateToken(token)) {
            try {
                TokenClaims claims = jwtPort.extractAllClaims(token);

                if (!"access".equals(claims.type())) {
                    log.warn("Invalid token type: expected access token | path={}", request.getRequestURI());
                    filterChain.doFilter(request, response);
                    return;
                }

                // ── INACTIVE user guard ──────────────────────────────────────
                if ("INACTIVE".equals(claims.status()) && List.of(ALLOW_PATHS_FOR_INACTIVE_USER).stream().noneMatch(path -> request.getRequestURI().startsWith(path))) {
                    log.warn("INACTIVE user blocked | userId={} path={}",
                            claims.subject(), request.getRequestURI());
                    writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, ErrorCode.USER_NOT_VERIFIED);
                    return;
                }
                // ─────────────────────────────────────────────────────────────

                UserId userId = UserId.of(Long.parseLong(claims.subject()));
                String role   = claims.role();

                var auth = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (Exception e) {
                log.warn("Cannot set authentication: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
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
