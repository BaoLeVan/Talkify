package com.talkify.identity.infrastructure.security;

import com.talkify.identity.application.port.OtpGeneratorPort;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class SecureOtpGeneratorAdapter implements OtpGeneratorPort {

    // SecureRandom là thread-safe và cryptographically strong
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public String generateCode() {
        // Sinh số từ 0 → 999999, format thành 6 chữ số với leading zero
        // Ví dụ: 42 → "000042", 391847 → "391847"
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }
}
