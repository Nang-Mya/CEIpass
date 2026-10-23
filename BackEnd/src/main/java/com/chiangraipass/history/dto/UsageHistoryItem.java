package com.chiangraipass.history.dto;

import java.time.Instant;

public record UsageHistoryItem(String attractionName, String passName, Instant usedAt, String status) {
}
