package com.chiangraipass.pass.entity;

import java.time.Instant;
import java.util.UUID;

import com.chiangraipass.catalog.entity.AttractionCatalogEntity;
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

    public BenefitUsageLogEntity() {
    }

    public UUID getUsageId() {
        return usageId;
    }

    public void setUsageId(UUID usageId) {
        this.usageId = usageId;
    }

    public UserPassEntity getUserPass() {
        return userPass;
    }

    public void setUserPass(UserPassEntity userPass) {
        this.userPass = userPass;
    }

    public AttractionCatalogEntity getPartner() {
        return partner;
    }

    public void setPartner(AttractionCatalogEntity partner) {
        this.partner = partner;
    }

    public BenefitEntity getBenefit() {
        return benefit;
    }

    public void setBenefit(BenefitEntity benefit) {
        this.benefit = benefit;
    }

    public Instant getCheckinDatetime() {
        return checkinDatetime;
    }

    public void setCheckinDatetime(Instant checkinDatetime) {
        this.checkinDatetime = checkinDatetime;
    }

    public String getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(String validationStatus) {
        this.validationStatus = validationStatus;
    }
}
