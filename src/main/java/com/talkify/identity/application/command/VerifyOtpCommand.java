package com.talkify.identity.application.command;

import com.talkify.identity.domain.model.OtpPurpose;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyOtpCommand(
    @NotBlank @Email String email,
    @NotBlank @Pattern(regexp = "\\d{6}", message = "OTP must be exactly 6 digits") String otp,
    OtpPurpose purpose
) {}
