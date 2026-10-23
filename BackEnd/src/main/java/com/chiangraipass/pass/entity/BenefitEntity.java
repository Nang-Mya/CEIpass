package com.chiangraipass.pass.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.chiangraipass.catalog.entity.AttractionCatalogEntity;
import com.chiangraipass.catalog.entity.PassCatalogEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "benefit", schema = "app")
public class BenefitEntity {

    @Id
    @Column(name = "benefit_id", nullable = false, length = 64)
    private String benefitId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_id", nullable = false)
    private AttractionCatalogEntity partner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pass_id", nullable = false)
    private PassCatalogEntity pass;

    @Column(name = "benefit_detail", nullable = false, length = 255)
    private String benefitDetail;

    @Column(name = "discount_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountRate = BigDecimal.ZERO;

    @Column(name = "usage_rule_limit", nullable = false)
    private int usageRuleLimit = 1;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public BenefitEntity() {
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public String getBenefitId() {
        return benefitId;
    }

    public void setBenefitId(String benefitId) {
        this.benefitId = benefitId;
    }

    public AttractionCatalogEntity getPartner() {
        return partner;
    }

    public void setPartner(AttractionCatalogEntity partner) {
        this.partner = partner;
    }

    public PassCatalogEntity getPass() {
        return pass;
    }

    public void setPass(PassCatalogEntity pass) {
        this.pass = pass;
    }

    public String getBenefitDetail() {
        return benefitDetail;
    }

    public void setBenefitDetail(String benefitDetail) {
        this.benefitDetail = benefitDetail;
    }

    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    public void setDiscountRate(BigDecimal discountRate) {
        this.discountRate = discountRate;
    }

    public int getUsageRuleLimit() {
        return usageRuleLimit;
    }

    public void setUsageRuleLimit(int usageRuleLimit) {
        this.usageRuleLimit = usageRuleLimit;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
