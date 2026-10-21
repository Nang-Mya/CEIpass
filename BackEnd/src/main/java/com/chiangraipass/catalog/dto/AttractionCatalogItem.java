package com.chiangraipass.catalog.dto;

public record AttractionCatalogItem(
        String partnerId,
        String name,
        String description,
        String location,
        String category,
        String imageUrl,
        String accessibilityInformation,
        boolean eligibleForPass) {
}
