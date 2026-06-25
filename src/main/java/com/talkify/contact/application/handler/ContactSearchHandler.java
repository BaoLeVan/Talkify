package com.talkify.contact.application.handler;

import com.talkify.common.domain.Email;
import com.talkify.common.domain.PhoneNumber;
import com.talkify.contact.application.command.SearchContactCommand;
import com.talkify.contact.domain.model.SearchType;

public class ContactSearchHandler {
    public void handleSearch(SearchContactCommand command) {
        
    }

    private SearchType detectSearchType(String query) {
        if (Email.isValid(query)) {
            return SearchType.EMAIL;
        } else if (PhoneNumber.isValid(query)) {
            return SearchType.PHONE_NUMBER;
        } else {
            return SearchType.USERNAME;
        }
    }
}
