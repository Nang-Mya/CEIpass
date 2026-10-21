package com.chiangraipass.pass.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.chiangraipass.auth.entity.VisitorAccountEntity;
import com.chiangraipass.catalog.entity.PassCatalogEntity;
import com.chiangraipass.payment.entity.PaymentEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_pass", schema = "app")
public class UserPassEntity {

    @Id
    @Column(name = "user_pass_id", nullable = false)
    private UUID userPassId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private VisitorAccountEntity account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pass_id", nullable = false)
    private PassCatalogEntity pass;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private PaymentEntity payment;

    @Column(name = "purchase_datetime", nullable = false)
    private Instant purchaseDatetime;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UserPassEntity() {
    }

    public UUID getUserPassId() {
        return userPassId;
    }

    public void setUserPassId(UUID userPassId) {
        this.userPassId = userPassId;
    }

    public VisitorAccountEntity getAccount() {
        return account;
    }

    public void setAccount(VisitorAccountEntity account) {
        this.account = account;
    }

    public PassCatalogEntity getPass() {
        return pass;
    }

    public void setPass(PassCatalogEntity pass) {
        this.pass = pass;
    }

    public PaymentEntity getPayment() {
        return payment;
    }

    public void setPayment(PaymentEntity payment) {
        this.payment = payment;
    }

    public Instant getPurchaseDatetime() {
        return purchaseDatetime;
    }

    public void setPurchaseDatetime(Instant purchaseDatetime) {
        this.purchaseDatetime = purchaseDatetime;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
