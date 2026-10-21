package com.chiangraipass.persistence.entity;

import java.time.Instant;

import com.chiangraipass.api.catalog.PassCatalogItem;
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

    @Column(name = "price_minor_units", nullable = false)
    private long priceMinorUnits;

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

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PassCatalogItem toCatalogItem() {
        return new PassCatalogItem(
                passId,
                displayName,
                description,
                priceMinorUnits,
                currency,
                validityDescription,
                available);
    }
}
