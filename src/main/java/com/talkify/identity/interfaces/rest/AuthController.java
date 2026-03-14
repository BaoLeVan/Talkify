package com.talkify.identity.interfaces.rest;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.talkify.common.security.SecurityUtils;
import com.talkify.dto.response.ApiResponse;
import com.talkify.identity.application.command.LoginCommand;
import com.talkify.identity.application.command.RegisterUserCommand;
import com.talkify.identity.application.command.ResendOtpCommand;
import com.talkify.identity.application.command.VerifyOtpCommand;
import com.talkify.identity.application.dto.response.AuthResponse;
import com.talkify.identity.application.handler.LoginHandler;
import com.talkify.identity.application.handler.OtpHandler;
import com.talkify.identity.application.handler.RegisterUserHandler;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final RegisterUserHandler registerUserHandler;
    private final LoginHandler loginHandler;
    private final OtpHandler otpHandler;

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginCommand command) {
        // Handler throw AppException cho các error case
        // GlobalExceptionHandler tự động set HTTP status đúng
        return ApiResponse.ok("Login successful", loginHandler.handle(command));
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterUserCommand command) {
        return ApiResponse.created("Registration successful, please verify your email", registerUserHandler.handle(command));
    }

    @PostMapping("/verify-otp") 
    public ApiResponse<Void> verifyOtp(@Valid @RequestBody VerifyOtpCommand command) {
        otpHandler.handle(command, SecurityUtils.requireCurrentUserId());
        return ApiResponse.ok("Verification successful", null);
    }

    @PostMapping("/resend-otp")
    public ApiResponse<Void> resendOtp(@Valid @RequestBody ResendOtpCommand command) {
        otpHandler.handle(command, SecurityUtils.requireCurrentUserId());
        return ApiResponse.ok("OTP resent successfully, please check your email", null);
    }
}
