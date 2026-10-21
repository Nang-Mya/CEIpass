package com.chiangraipass.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
    private BigDecimal discountRate;

    @Column(name = "usage_rule_limit", nullable = false)
    private int usageRuleLimit;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
