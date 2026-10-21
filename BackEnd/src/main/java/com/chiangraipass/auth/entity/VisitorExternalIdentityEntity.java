package com.chiangraipass.auth.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "visitor_external_identity", schema = "app", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_visitor_external_identity_account_provider",
                columnNames = { "account_id", "provider" })
})
@IdClass(VisitorExternalIdentityId.class)
public class VisitorExternalIdentityEntity {

    @Id
    @Column(name = "provider", nullable = false, length = 32)
    private String provider;

    @Id
    @Column(name = "provider_subject", nullable = false, length = 255)
    private String providerSubject;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private VisitorAccountEntity account;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public void setProviderSubject(String providerSubject) {
        this.providerSubject = providerSubject;
    }

    public void setAccount(VisitorAccountEntity account) {
        this.account = account;
    }
}
