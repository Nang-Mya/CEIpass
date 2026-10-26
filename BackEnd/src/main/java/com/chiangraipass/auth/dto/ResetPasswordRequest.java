package com.chiangraipass.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Reset token is required.")
        @Size(max = 128, message = "The reset link is not valid.")
        String token,
        @NotBlank(message = "New password is required.")
        @Size(max = 256, message = "Password is too long.")
        String newPassword) {
}