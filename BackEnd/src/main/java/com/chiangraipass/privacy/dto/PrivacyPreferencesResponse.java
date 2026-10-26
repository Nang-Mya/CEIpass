package com.chiangraipass.privacy.dto;

import java.time.Instant;
import java.util.List;

import com.chiangraipass.privacy.model.PrivacyPurpose;

public record PrivacyPreferencesResponse(
        boolean available, String noticeVersion, String noticeUrl, List<Item> preferences) {

    public record Item(PrivacyPurpose purpose, boolean granted, Instant recordedAt) {
    }
}