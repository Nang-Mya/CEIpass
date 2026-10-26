package com.chiangraipass.pass.dto;

import java.time.Instant;
import java.util.UUID;

public record QrPassResponse(
        UUID accountId,
        UUID userPassId,
        String passName,
        Instant expiresAt,
        String qrImageDataUrl,
        String qrPayload
) {
}