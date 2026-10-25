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

    /** Legacy compatibility constructor for older tests and callers. */
    public VisitorAccount(UUID accountId, String email, String displayName, String passwordHash) {
        this(accountId, email, displayName, passwordHash, true, null, null);
    }

    /** Compatibility constructor: treated as verified and unlocked. */
    public VisitorAccount(UUID accountId, String email, String displayName, String passwordHash, Instant lockedUntil) {
        this(accountId, email, displayName, passwordHash, true, lockedUntil, null);
    }

    /** Compatibility constructor used by older account fixtures. */
    public VisitorAccount(UUID accountId, String email, String displayName, String passwordHash, boolean emailVerified, Instant lockedUntil) {
        this(accountId, email, displayName, passwordHash, emailVerified, lockedUntil, null);
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