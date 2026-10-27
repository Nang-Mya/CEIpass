package com.chiangraipass.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required.")
        @Size(max = 256, message = "Password is too long.")
        String currentPassword,
        @NotBlank(message = "New password is required.")
        @Size(max = 256, message = "Password is too long.")
        String newPassword) {
}