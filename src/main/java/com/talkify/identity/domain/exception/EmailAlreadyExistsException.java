package com.talkify.identity.domain.exception;

import com.talkify.common.exception.AppException;
import com.talkify.common.exception.ErrorCode;

public class EmailAlreadyExistsException extends AppException {
    public EmailAlreadyExistsException(String email) {
        super(ErrorCode.EMAIL_ALREADY_EXISTS, "Email already exists: " + email);
    }
}
