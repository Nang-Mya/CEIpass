package com.chiangraipass.auth.model;

import java.time.Instant;
import java.util.UUID;

public record VisitorAccount(
        UUID accountId,
        String email,
        String displayName,
        String passwordHash,
        boolean emailVerified,
        Instant lockedUntil) {

    /** Compatibility constructor: treated as verified and unlocked. */
    public VisitorAccount(UUID accountId, String email, String displayName, String passwordHash) {
        this(accountId, email, displayName, passwordHash, true, null);
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }
}