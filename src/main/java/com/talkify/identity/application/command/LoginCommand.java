package com.talkify.identity.application.command;

import jakarta.validation.constraints.NotBlank;

public record LoginCommand(
    @NotBlank String identifier, 
    @NotBlank String password) {}
