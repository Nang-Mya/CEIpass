package com.chiangraipass.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "benefit_usage_log", schema = "app")
public class BenefitUsageLogEntity {

    @Id
    @Column(name = "usage_id", nullable = false)
    private UUID usageId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_pass_id", nullable = false)
    private UserPassEntity userPass;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_id", nullable = false)
    private AttractionCatalogEntity partner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "benefit_id", nullable = false)
    private BenefitEntity benefit;

    @Column(name = "checkin_datetime", nullable = false)
    private Instant checkinDatetime;

    @Column(name = "validation_status", nullable = false, length = 20)
    private String validationStatus;
}
