package com.chiangraipass.auth.entity;

import java.time.Instant;
import java.util.UUID;

import com.chiangraipass.auth.model.AccountTokenPurpose;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** A single-use token (email verification, password reset). Only its SHA-256 hash is stored. */
@Entity
@Table(name = "account_token", schema = "app",
        uniqueConstraints = @UniqueConstraint(name = "UQ_account_token_hash", columnNames = "token_hash"),
        indexes = @Index(name = "IX_account_token_account", columnList = "account_id, purpose"))
public class AccountTokenEntity {

    @Id
    @Column(name = "token_id", nullable = false)
    private UUID tokenId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 32)
    private AccountTokenPurpose purpose;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AccountTokenEntity() {
    }

    public AccountTokenEntity(UUID accountId, AccountTokenPurpose purpose, String tokenHash,
            Instant createdAt, Instant expiresAt) {
        this.tokenId = UUID.randomUUID();
        this.accountId = accountId;
        this.purpose = purpose;
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public UUID getTokenId() {
        return tokenId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public AccountTokenPurpose getPurpose() {
        return purpose;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getConsumedAt() {
        return consumedAt;
    }
}