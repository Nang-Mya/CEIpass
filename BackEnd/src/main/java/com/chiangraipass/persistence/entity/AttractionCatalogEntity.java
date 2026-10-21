package com.chiangraipass.persistence.entity;

import java.time.Instant;

import com.chiangraipass.api.catalog.AttractionCatalogItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "attraction_catalog", schema = "app")
public class AttractionCatalogEntity {

    @Id
    @Column(name = "partner_id", nullable = false, length = 64)
    private String partnerId;

    @Column(name = "public_name", nullable = false, length = 160)
    private String publicName;

    @Column(name = "location", nullable = false, length = 240)
    private String location;

    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @Column(name = "image_url", length = 2048)
    private String imageUrl;

    @Column(name = "accessibility_information", length = 1000)
    private String accessibilityInformation;

    @Column(name = "is_pass_eligible", nullable = false)
    private boolean passEligible;

    @Column(name = "is_published", nullable = false)
    private boolean published;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AttractionCatalogItem toCatalogItem() {
        return new AttractionCatalogItem(
                partnerId,
                publicName,
                location,
                category,
                imageUrl,
                accessibilityInformation,
                passEligible);
    }
}
