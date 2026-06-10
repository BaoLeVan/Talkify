package com.talkify.contact.domain.exception;

import com.talkify.common.exception.AppException;
import com.talkify.common.exception.ErrorCode;

/**
 * Domain exception: the acting user does not have permission to perform
 * the requested operation on a contact request (e.g., accepting a request
 * that was not addressed to them).
 */
public class ContactPermissionException extends AppException {
    public ContactPermissionException() {
        super(ErrorCode.CONTACT_ACTION_FORBIDDEN,
              "Don't have permission to perform action");
    }
}
