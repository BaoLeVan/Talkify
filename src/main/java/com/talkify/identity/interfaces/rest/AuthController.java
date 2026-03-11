package com.talkify.identity.interfaces.rest;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.talkify.dto.response.ApiResponse;
import com.talkify.identity.application.command.RegisterUserCommand;
import com.talkify.identity.application.handler.RegisterUserHandler;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final RegisterUserHandler registerUserHandler;

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterUserCommand command) {
        registerUserHandler.handle(command);
        return ApiResponse.created("User registered successfully", null);
    }
}
