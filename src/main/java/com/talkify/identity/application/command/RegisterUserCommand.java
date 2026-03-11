package com.talkify.identity.application.command;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserCommand(
    @NotBlank @Email String email,
    @NotBlank String username,
    @NotBlank @Size(min = 8) String password,
    @NotBlank @Size(min = 8) String confirmPassword,
    @NotBlank String displayName
) {}
    