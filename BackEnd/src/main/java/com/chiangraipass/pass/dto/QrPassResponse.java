package com.chiangraipass.pass.dto;

import java.util.UUID;

public record QrPassResponse(
        UUID accountId,
        UUID userPassId,
        String passName,
        String holderName,
        String qrImageDataUrl,
        String qrPayload
) {
}