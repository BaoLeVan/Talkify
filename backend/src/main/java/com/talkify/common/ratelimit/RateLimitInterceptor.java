package com.talkify.common.ratelimit;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.talkify.common.exception.ErrorCode;
import com.talkify.common.security.AuthPrincipal;
import com.talkify.dto.response.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final String KEY_PREFIX = "rl:";

    private final StringRedisTemplate redis;
    private final ObjectMapper        objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RateLimit annotation = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (annotation == null) {
            return true;
        }

        String key   = buildKey(annotation, request);
        Long   count = redis.opsForValue().increment(key);

        if (count == null) {
            log.error("Redis unavailable — skipping rate limit | key={}", key);
            return true;
        }

        if (count == 1L) {
            redis.expire(key, Duration.ofSeconds(annotation.windowSeconds()));
        }

        if (count > annotation.limit()) {
            log.warn("Rate limit exceeded | key={} count={} limit={} window={}s",
                    key, count, annotation.limit(), annotation.windowSeconds());
            write429(response);
            return false;
        }

        return true;
    }

    private String buildKey(RateLimit rl, HttpServletRequest request) {
        String uri     = request.getRequestURI();
        String subject = (rl.by() == RateLimitKey.USER)
                ? resolveUserId()
                : resolveClientIp(request);
        return KEY_PREFIX + rl.by().name().toLowerCase() + ":" + uri + ":" + subject;
    }

    private String resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthPrincipal principal) {
            return String.valueOf(principal.userId().value());
        }
        log.warn("RateLimitInterceptor: USER key requested but no AuthPrincipal in SecurityContext");
        return "anonymous";
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void write429(HttpServletResponse response) throws Exception {
        response.setStatus(429); // TOO_MANY_REQUESTS
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getOutputStream(),
                ApiResponse.error(ErrorCode.RATE_LIMIT_EXCEEDED)
        );
    }
}
