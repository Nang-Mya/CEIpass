package com.chiangraipass.history.service;

import java.util.List;
import java.util.UUID;

import com.chiangraipass.history.dto.UsageHistoryItem;
import com.chiangraipass.pass.repository.BenefitUsageLogJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsageHistoryService {

    static final int DEFAULT_LIMIT = 50;
    static final int MAX_LIMIT = 100;

    private final BenefitUsageLogJpaRepository usageLogRepository;

    public UsageHistoryService(BenefitUsageLogJpaRepository usageLogRepository) {
        this.usageLogRepository = usageLogRepository;
    }

    @Transactional(readOnly = true)
    public List<UsageHistoryItem> recentUsage(UUID accountId, int limit) {
        int size = Math.min(Math.max(limit, 1), MAX_LIMIT);
        return usageLogRepository.findRecentUsage(accountId, PageRequest.of(0, size));
    }
}
