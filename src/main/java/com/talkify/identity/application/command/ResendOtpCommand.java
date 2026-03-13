package com.talkify.identity.application.command;

import com.talkify.identity.domain.model.OtpPurpose;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendOtpCommand(
    @NotBlank @Email String email,
    @NotBlank OtpPurpose purpose
) {}
