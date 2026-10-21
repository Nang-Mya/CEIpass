package com.chiangraipass.catalog.entity;

import java.time.Instant;

import com.chiangraipass.catalog.dto.AttractionCatalogItem;
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

    @Column(name = "description", length = 1000)
    private String description;

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

    public AttractionCatalogEntity() {
    }

    public AttractionCatalogItem toCatalogItem() {
        return new AttractionCatalogItem(
                partnerId,
                publicName,
                description,
                location,
                category,
                imageUrl,
                accessibilityInformation,
                passEligible);
    }

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

    public String getPublicName() {
        return publicName;
    }

    public void setPublicName(String publicName) {
        this.publicName = publicName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getAccessibilityInformation() {
        return accessibilityInformation;
    }

    public void setAccessibilityInformation(String accessibilityInformation) {
        this.accessibilityInformation = accessibilityInformation;
    }

    public boolean isPassEligible() {
        return passEligible;
    }

    public void setPassEligible(boolean passEligible) {
        this.passEligible = passEligible;
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
