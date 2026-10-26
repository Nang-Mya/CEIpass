package com.chiangraipass.privacy.entity;

import java.time.Instant;
import java.util.UUID;

import com.chiangraipass.privacy.model.PrivacyPurpose;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** One recorded choice. Rows are only ever added: the latest row per purpose is the current choice. */
@Entity
@Immutable
@Table(name = "privacy_preference", schema = "app",
        indexes = @Index(name = "IX_privacy_preference_account", columnList = "account_id, id"))
public class PrivacyPreferenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 40)
    private PrivacyPurpose purpose;

    @Column(name = "granted", nullable = false)
    private boolean granted;

    @Column(name = "notice_version", nullable = false, length = 80)
    private String noticeVersion;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected PrivacyPreferenceEntity() {
    }

    public PrivacyPreferenceEntity(
            UUID accountId, PrivacyPurpose purpose, boolean granted, String noticeVersion, Instant recordedAt) {
        this.accountId = accountId;
        this.purpose = purpose;
        this.granted = granted;
        this.noticeVersion = noticeVersion;
        this.recordedAt = recordedAt;
    }

    public Long getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public PrivacyPurpose getPurpose() {
        return purpose;
    }

    public boolean isGranted() {
        return granted;
    }

    public String getNoticeVersion() {
        return noticeVersion;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}