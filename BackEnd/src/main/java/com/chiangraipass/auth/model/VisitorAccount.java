package com.chiangraipass.auth.model;

import java.time.Instant;
import java.util.UUID;

public record VisitorAccount(
        UUID accountId,
        String email,
        String displayName,
        String passwordHash,
        boolean emailVerified,
        Instant lockedUntil,
        String securityToken) {

    /** Compatibility constructor: treated as verified and unlocked. */
    public VisitorAccount(UUID accountId, String email, String displayName, String passwordHash, Instant lockedUntil) {
        this(accountId, email, displayName, passwordHash, true, lockedUntil, null);
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    /** Keeps the password hash and security token out of logs. */
    @Override
    public String toString() {
        return "VisitorAccount[accountId=" + accountId + "]";
    }
}