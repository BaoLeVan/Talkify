package com.talkify.identity.domain.model;

import com.talkify.common.exception.AppException;
import com.talkify.common.exception.ErrorCode;

public record OtpCacheKey(String value) {

    private static final String OTP_PREFIX = "otp:";
    private static final String ATTEMPT_PREFIX = "otp_attempt:";

    public OtpCacheKey {
        if (value == null || value.isBlank()) {
            throw new AppException(ErrorCode.OTP_MUST_NOT_BE_NULL);
        }
    }

    public static OtpCacheKey of(UserId userId, OtpPurpose purpose) {
        return new OtpCacheKey(OTP_PREFIX + userId.value() + ":" + purpose.name());
    }

    public static OtpCacheKey attemptOf(UserId userId, OtpPurpose purpose) {
        return new OtpCacheKey(ATTEMPT_PREFIX + userId.value() + ":" + purpose.name());
    }
}
