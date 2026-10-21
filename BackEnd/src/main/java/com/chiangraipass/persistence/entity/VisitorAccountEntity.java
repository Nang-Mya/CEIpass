package com.chiangraipass.persistence.entity;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import com.chiangraipass.api.auth.VisitorAccount;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "visitor_account", schema = "app")
public class VisitorAccountEntity {

    private static final SecureRandom TOKEN_RANDOM = new SecureRandom();

    @Id
    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "normalized_email", nullable = false, length = 254)
    private String normalizedEmail;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "privacy_notice_version", nullable = false, length = 80)
    private String privacyNoticeVersion;

    @Column(name = "privacy_notice_acknowledged_at", nullable = false)
    private Instant privacyNoticeAcknowledgedAt;

    @Column(name = "account_status", nullable = false, length = 16)
    private String accountStatus;

    @Column(name = "security_token", nullable = false, length = 64)
    private String securityToken;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<VisitorExternalIdentityEntity> identities = new HashSet<>();

    public static VisitorAccountEntity newEmailAccount(
            UUID accountId,
            String email,
            String displayName,
            String passwordHash,
            String privacyNoticeVersion) {
        VisitorAccountEntity account = baseAccount(accountId, email, displayName, privacyNoticeVersion);
        account.passwordHash = passwordHash;
        return account;
    }

    public static VisitorAccountEntity newOAuthAccount(
            UUID accountId,
            String email,
            String displayName,
            String provider,
            String subject,
            String privacyNoticeVersion) {
        VisitorAccountEntity account = baseAccount(accountId, email, displayName, privacyNoticeVersion);
        VisitorExternalIdentityEntity identity = new VisitorExternalIdentityEntity();
        identity.setProvider(provider);
        identity.setProviderSubject(subject);
        identity.setAccount(account);
        account.identities.add(identity);
        return account;
    }

    private static VisitorAccountEntity baseAccount(
            UUID accountId,
            String email,
            String displayName,
            String privacyNoticeVersion) {
        VisitorAccountEntity account = new VisitorAccountEntity();
        account.accountId = accountId;
        account.email = email;
        account.normalizedEmail = email.strip().toLowerCase(Locale.ROOT);
        account.displayName = displayName;
        account.privacyNoticeVersion = privacyNoticeVersion;
        account.privacyNoticeAcknowledgedAt = Instant.now();
        account.accountStatus = "ACTIVE";
        account.securityToken = generateSecurityToken();
        return account;
    }

    public static String generateSecurityToken() {
        byte[] bytes = new byte[32];
        TOKEN_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (accountStatus == null) {
            accountStatus = "ACTIVE";
        }
        if (securityToken == null) {
            securityToken = generateSecurityToken();
        }
        if (privacyNoticeAcknowledgedAt == null) {
            privacyNoticeAcknowledgedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public VisitorAccount toAccount() {
        return new VisitorAccount(accountId, email, displayName, passwordHash);
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Set<VisitorExternalIdentityEntity> getIdentities() {
        return identities;
    }
}
