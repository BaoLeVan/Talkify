package com.talkify.identity.domain.event;

import com.talkify.identity.domain.model.OtpPurpose;

/**
 * Domain event raised sau khi user đăng ký thành công.
 * Listener (AFTER_COMMIT) sẽ gửi OTP vào email.
 */
public record UserRegistedEvent(
        String email,
        String displayName,
        OtpPurpose otpPurpose
) {}
