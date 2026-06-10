package com.talkify.contact.domain.exception;

import com.talkify.common.exception.AppException;
import com.talkify.common.exception.ErrorCode;

/**
 * Domain exception: an action (accept/reject/cancel) is attempted on a Contact
 * that is not in the PENDING status, violating the aggregate's status-transition invariant.
 */
public class ContactStatusViolationException extends AppException {
    public ContactStatusViolationException() {
        super(ErrorCode.CONTACT_REQUEST_NOT_PENDING,
              "Cannot perform action");
    }
}
