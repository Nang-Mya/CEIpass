package com.chiangraipass.history.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PassSummaryResponse(PassInfo pass, List<BenefitQuota> benefits, List<UsageHistoryItem> recentUsage) {

    public record PassInfo(
            String passName,
            String status,
            LocalDate startDate,
            LocalDate endDate,
            Instant endsAt,
            String displayStatus,
            Instant startsAt) {
    }

    public record BenefitQuota(String attractionName, String detail, long used, int limit) {
    }
}
