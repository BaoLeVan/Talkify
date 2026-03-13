package com.talkify.identity.application.handler;

import java.time.Duration;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.talkify.common.exception.AppException;
import com.talkify.common.exception.ErrorCode;
import com.talkify.identity.application.command.SendOtpCommand;
import com.talkify.identity.application.command.VerifyOtpCommand;
import com.talkify.identity.application.port.CachePort;
import com.talkify.identity.application.port.EmailPort;
import com.talkify.identity.application.port.OtpGeneratorPort;
import com.talkify.identity.domain.model.Email;
import com.talkify.identity.domain.model.OtpCacheKey;
import com.talkify.identity.domain.model.User;
import com.talkify.identity.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpHandler {

    private static final Duration OTP_TTL        = Duration.ofMinutes(5);
    private static final Duration ATTEMPT_TTL    = Duration.ofMinutes(10);
    private static final int      MAX_ATTEMPTS   = 5;

    private final UserRepository userRepository;
    private final CachePort cachePort;
    private final EmailPort emailPort;
    private final OtpGeneratorPort otpGeneratorPort;

    @Transactional(readOnly = true)
    public void handle(SendOtpCommand command) {
        User user = userRepository.findByEmail(new Email(command.email()))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        String code = otpGeneratorPort.generateCode();
        String cacheKey = OtpCacheKey.of(user.getId(), command.purpose()).value();

        cachePort.set(cacheKey, code, OTP_TTL);
        cachePort.delete(OtpCacheKey.attemptOf(user.getId(), command.purpose()).value());

        log.info("OTP sent | userId={} purpose={}", user.getId().value(), command.purpose());

        emailPort.sendVerificationEmail(user.getEmail().value(), user.getDisplayName(), code);
    }

    @Transactional
    public void handle(VerifyOtpCommand command) {
        User user = userRepository.findByEmail(new Email(command.email()))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        String otpKey     = OtpCacheKey.of(user.getId(), command.purpose()).value();
        String attemptKey = OtpCacheKey.attemptOf(user.getId(), command.purpose()).value();

        long attempts = cachePort.increment(attemptKey, ATTEMPT_TTL);
        if (attempts > MAX_ATTEMPTS) {
            log.warn("OTP brute-force | userId={} attempts={}", user.getId().value(), attempts);
            throw new AppException(ErrorCode.OTP_TOO_MANY_ATTEMPTS);
        }

        String cachedCode = cachePort.getAndDelete(otpKey)
                .orElseThrow(() -> new AppException(ErrorCode.OTP_EXPIRED));

        if (!cachedCode.equals(command.otp())) {
            throw new AppException(ErrorCode.OTP_INVALID);
        }

        user.activate();
        userRepository.save(user);
        cachePort.delete(attemptKey);

        log.info("User activated | userId={}", user.getId().value());
    }
}
