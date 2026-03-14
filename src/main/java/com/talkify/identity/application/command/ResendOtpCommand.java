package com.talkify.identity.application.command;

import com.talkify.identity.domain.model.OtpPurpose;

import jakarta.validation.constraints.NotNull;

public record ResendOtpCommand(
    @NotNull OtpPurpose purpose
) {}
