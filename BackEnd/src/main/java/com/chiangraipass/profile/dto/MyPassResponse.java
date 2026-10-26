package com.chiangraipass.profile.dto;

import java.time.LocalDate;
import java.util.List;

public record MyPassResponse(PassDetails pass) {

    public record PassDetails(
            String passName,
            String status,
            LocalDate startDate,
            LocalDate endDate,
            List<BenefitUsage> benefits) {
    }

    public record BenefitUsage(String placeName, String detail, int used, int limit) {
    }
}