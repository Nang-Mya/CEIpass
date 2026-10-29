package com.chiangraipass.history.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PassCardResponse(
        UUID userPassId,
        String passName,
        String holderName,
        Instant purchaseDatetime,
        LocalDate startDate,
        LocalDate endDate,
        String displayStatus,
        Instant startsAt,
        Instant endsAt,
        List<BenefitQuota> benefits,
        List<UsageHistoryItem> recentUsage) {

    public record BenefitQuota(String attractionName, String detail, long used, int limit) {
    }
}
