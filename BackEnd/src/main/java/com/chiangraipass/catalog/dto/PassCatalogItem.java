package com.chiangraipass.catalog.dto;

import java.math.BigDecimal;

public record PassCatalogItem(
        String id,
        String displayName,
        String description,
        BigDecimal price,
        String currency,
        String validityDescription,
        boolean available) {
}
