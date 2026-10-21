package com.chiangraipass.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OAuthPreparationRequest(
        @NotBlank @Size(max = 16) String provider,
        boolean privacyNoticeAcknowledged) {
}
