package com.talkify.identity.interfaces.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.talkify.common.exception.AppException;
import com.talkify.common.exception.ErrorCode;
import com.talkify.common.security.SecurityUtils;
import com.talkify.dto.response.ApiResponse;
import com.talkify.identity.application.command.LoginCommand;
import com.talkify.identity.application.command.LogoutCommand;
import com.talkify.identity.application.command.RefreshTokenCommand;
import com.talkify.identity.application.command.RegisterUserCommand;
import com.talkify.identity.application.command.ResendOtpCommand;
import com.talkify.identity.application.command.VerifyOtpCommand;
import com.talkify.identity.application.dto.response.AuthResponse;
import com.talkify.identity.application.handler.LoginHandler;
import com.talkify.identity.application.handler.LogoutHandler;
import com.talkify.identity.application.handler.OtpHandler;
import com.talkify.identity.application.handler.RegisterUserHandler;
import com.talkify.identity.application.handler.SessionHandler;
import com.talkify.identity.application.port.JwtPort;
import com.talkify.identity.domain.model.DeviceInfo;
import com.talkify.identity.domain.model.SessionId;
import com.talkify.identity.domain.model.UserId;
import com.talkify.identity.interfaces.rest.dto.LogoutRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterUserHandler      registerUserHandler;
    private final SessionHandler           sessionHandler;
    private final LoginHandler             loginHandler;
    private final LogoutHandler            logoutHandler;
    private final OtpHandler               otpHandler;
    private final DeviceContextExtractor   deviceContextExtractor;
    private final RefreshTokenCookieHelper cookieHelper;
    private final JwtPort                  jwtPort;

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginCommand command,
                                           HttpServletRequest request,
                                           HttpServletResponse response) {
        DeviceInfo deviceInfo = deviceContextExtractor.extract(request);
        AuthResponse result   = loginHandler.handle(command, deviceInfo);
        cookieHelper.setRefreshTokenCookie(response, result.refreshToken());
        return ApiResponse.ok("Login successful", AuthResponse.withoutToken(result));
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterUserCommand command,
                                              HttpServletRequest request,
                                              HttpServletResponse response) {
        DeviceInfo deviceInfo = deviceContextExtractor.extract(request);
        AuthResponse result   = registerUserHandler.handle(command, deviceInfo);
        cookieHelper.setRefreshTokenCookie(response, result.refreshToken());
        return ApiResponse.created("Registration successful, please verify your email",
                AuthResponse.withoutToken(result));
    }

    @GetMapping("/refresh-token")
    public ApiResponse<AuthResponse> refreshToken(HttpServletRequest request,
                                                  HttpServletResponse response) {
        String rawToken   = cookieHelper.extractRefreshToken(request)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_TOKEN));
        DeviceInfo deviceInfo = deviceContextExtractor.extract(request);
        AuthResponse result   = sessionHandler.handle(new RefreshTokenCommand(rawToken), deviceInfo);
        cookieHelper.setRefreshTokenCookie(response, result.refreshToken());
        return ApiResponse.ok("Token refreshed successfully", AuthResponse.withoutToken(result));
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

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request,
                                    HttpServletResponse response,
                                    @Valid @RequestBody LogoutRequest body) {
        cookieHelper.clearRefreshTokenCookie(response);

        String rawToken = extractBearerToken(request);
        if (rawToken == null) {
            return ApiResponse.ok("Logout successful", null);
        }

        jwtPort.extractClaimsIgnoreExpiry(rawToken).ifPresent(claims -> {
            UserId    userId    = UserId.of(Long.parseLong(claims.subject()));
            SessionId sessionId = claims.sessionId();
            logoutHandler.handle(new LogoutCommand(sessionId, body.scope()), userId);
        });

        return ApiResponse.ok("Logout successful", null);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
