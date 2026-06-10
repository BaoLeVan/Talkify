package com.talkify.identity.domain.exception;

import com.talkify.common.exception.AppException;
import com.talkify.common.exception.ErrorCode;
import com.talkify.identity.domain.model.UserStatus;

/**
 * Domain exception: User.activate() is called on an account whose status
 * does not allow activation (only INACTIVE accounts may be activated).
 */
public class AccountActivationException extends AppException {
    public AccountActivationException(UserStatus currentStatus) {
        super(ErrorCode.ACCOUNT_NOT_ACTIVATABLE,
              "Only INACTIVE accounts can be activated (current status: %s)".formatted(currentStatus));
    }
}
