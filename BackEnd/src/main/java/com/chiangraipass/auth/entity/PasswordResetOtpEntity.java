package com.chiangraipass.auth.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** A one-time password-reset code. Only a BCrypt hash of the 6-digit code is stored. */
@Entity
@Table(name = "password_reset_otp", schema = "app",
        indexes = @Index(name = "IX_password_reset_otp_account", columnList = "account_id, created_at"))
public class PasswordResetOtpEntity {

    @Id
    @Column(name = "otp_id", nullable = false)
    private UUID otpId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "otp_hash", nullable = false, length = 100)
    private String otpHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PasswordResetOtpEntity() {
    }

    public PasswordResetOtpEntity(UUID accountId, String otpHash, Instant createdAt, Instant expiresAt) {
        this.otpId = UUID.randomUUID();
        this.accountId = accountId;
        this.otpHash = otpHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getOtpHash() {
        return otpHash;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public Instant getConsumedAt() {
        return consumedAt;
    }

    public void recordFailure() {
        failedAttempts++;
    }

    public void markConsumed(Instant now) {
        consumedAt = now;
    }
}