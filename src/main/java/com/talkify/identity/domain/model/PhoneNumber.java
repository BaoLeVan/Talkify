package com.talkify.identity.domain.model;

import com.talkify.common.exception.AppException;
import com.talkify.common.exception.ErrorCode;

public record PhoneNumber(String value) {
    public PhoneNumber {
        if (value == null || value.trim().isEmpty()) {
            throw new AppException(ErrorCode.PHONE_NUMBER_MUST_NOT_BE_NULL);
        }
        if (!value.matches("\\+?[0-9]{7,15}")) {
            throw new AppException(ErrorCode.PHONE_NUMBER_INVALID);
        }
    }

    public static PhoneNumber of(String value) {
        return new PhoneNumber(value);
    }  
    
    public static boolean isValid(String value) {
        return value != null && value.matches("\\+?[0-9]{7,15}");
    }
}
