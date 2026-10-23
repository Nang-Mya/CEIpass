package com.chiangraipass.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyEmailRequest(
        @NotBlank(message = "Verification token is required.")
        @Size(max = 128, message = "The verification link is not valid.")
        String token) {
}