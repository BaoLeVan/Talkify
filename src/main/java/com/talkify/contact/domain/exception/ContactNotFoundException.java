package com.talkify.contact.domain.exception;

import com.talkify.common.exception.AppException;
import com.talkify.common.exception.ErrorCode;

public class ContactNotFoundException extends AppException {
    public ContactNotFoundException() {
        super(ErrorCode.CONTACT_NOT_FOUND);
    }
    
}
