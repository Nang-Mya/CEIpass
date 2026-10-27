package com.chiangraipass.profile.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A visitor's profile picture: one small JPEG per account, kept apart from the account row. */
@Entity
@Table(name = "visitor_avatar", schema = "app")
public class VisitorAvatarEntity {

    @Id
    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "image_data", nullable = false, length = 262144)
    private byte[] imageData;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected VisitorAvatarEntity() {
    }

    public VisitorAvatarEntity(UUID accountId, byte[] imageData, Instant updatedAt) {
        this.accountId = accountId;
        this.imageData = imageData;
        this.updatedAt = updatedAt;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}