package com.talkify.contact.interfaces;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.talkify.common.domain.UserId;
import com.talkify.common.security.SecurityUtils;
import com.talkify.contact.application.command.SentContactRequestCommand;
import com.talkify.contact.application.handler.ContactRequestActionHandler;
import com.talkify.contact.application.handler.ContactRequestHandler;
import com.talkify.contact.interfaces.request.ContactRequest;
import com.talkify.dto.response.ApiResponse;

import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/v1/contacts")
@RequiredArgsConstructor
public class ContactController {
    private final ContactRequestHandler contactRequestHandler;
    private final ContactRequestActionHandler contactRequestActionHandler;
    
    @PostMapping("/request")
    public ApiResponse<String> sendContactRequest(@RequestBody ContactRequest request) {
        UserId requesterId = SecurityUtils.requireCurrentUserId();
        return contactRequestHandler.handle(new SentContactRequestCommand(requesterId.value(), request.addresseeId()));
    }

    @PostMapping("/request/{requestId}/accept")
    public ApiResponse<String> respondToContactRequest(
        @PathVariable String requestId
    ) {
        contactRequestActionHandler.handleAcceptance(requestId, SecurityUtils.requireCurrentUserId());
        return ApiResponse.ok("Contact request accepted successfully");
    }

    @PostMapping("/request/{requestId}/reject")
    public ApiResponse<String> rejectContactRequest(
        @PathVariable String requestId
    ) {
        contactRequestActionHandler.handleRejection(requestId, SecurityUtils.requireCurrentUserId());
        return ApiResponse.ok("Contact request rejected successfully");
    }

    @PostMapping("/request/{requestId}/cancel")
    public ApiResponse<String> cancelContactRequest(
        @PathVariable String requestId
    ) {
        contactRequestActionHandler.handleCancellation(requestId, SecurityUtils.requireCurrentUserId());
        return ApiResponse.ok("Contact request canceled successfully");
    }
}
