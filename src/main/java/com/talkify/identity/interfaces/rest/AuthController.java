package com.talkify.identity.interfaces.rest;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.talkify.dto.response.ApiResponse;
import com.talkify.identity.application.command.RegisterUserCommand;
import com.talkify.identity.application.command.ResendOtpCommand;
import com.talkify.identity.application.command.VerifyOtpCommand;
import com.talkify.identity.application.handler.OtpHandler;
import com.talkify.identity.application.handler.RegisterUserHandler;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final RegisterUserHandler registerUserHandler;
    private final OtpHandler otpHandler;

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterUserCommand command) {
        registerUserHandler.handle(command);
        return ApiResponse.created("User registered successfully, please check your email for OTP", null);
    }

    @PostMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(@Valid @RequestBody VerifyOtpCommand command) {
        otpHandler.handle(command);
        return ApiResponse.ok(null);
    }

    @PostMapping("/resend-otp")
    public ApiResponse<Void> resendOtp(@Valid @RequestBody ResendOtpCommand command) {
        otpHandler.handle(command);
        return ApiResponse.ok(null);
    }
}
