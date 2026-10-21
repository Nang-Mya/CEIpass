package com.chiangraipass.catalog.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.chiangraipass.catalog.dto.PassCatalogItem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pass_catalog", schema = "app")
public class PassCatalogEntity {

    @Id
    @Column(name = "pass_id", nullable = false, length = 64)
    private String passId;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "validity_description", nullable = false, length = 160)
    private String validityDescription;

    @Column(name = "is_available", nullable = false)
    private boolean available;

    @Column(name = "is_published", nullable = false)
    private boolean published;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PassCatalogEntity() {
    }

    public PassCatalogItem toCatalogItem() {
        return new PassCatalogItem(
                passId,
                displayName,
                description,
                price,
                currency,
                validityDescription,
                available);
    }

    public String getPassId() {
        return passId;
    }

    public void setPassId(String passId) {
        this.passId = passId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getValidityDescription() {
        return validityDescription;
    }

    public void setValidityDescription(String validityDescription) {
        this.validityDescription = validityDescription;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
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
