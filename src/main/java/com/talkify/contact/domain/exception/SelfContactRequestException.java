package com.talkify.contact.domain.exception;

import com.talkify.common.exception.AppException;
import com.talkify.common.exception.ErrorCode;

/**
 * Domain exception: a user attempts to send a contact request to themselves.
 * Invariant enforced in Contact.createRequest().
 */
public class SelfContactRequestException extends AppException {
    public SelfContactRequestException() {
        super(ErrorCode.CONTACT_SELF_REQUEST);
    }
}
