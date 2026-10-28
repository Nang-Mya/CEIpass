package com.chiangraipass.history.service;

import java.util.List;
import java.util.UUID;

import com.chiangraipass.history.dto.UsageHistoryItem;
import com.chiangraipass.pass.repository.BenefitUsageLogJpaRepository;
import com.chiangraipass.pass.repository.UserPassJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsageHistoryService {

    static final int DEFAULT_LIMIT = 50;
    static final int MAX_LIMIT = 100;

    private final BenefitUsageLogJpaRepository usageLogRepository;
    private final UserPassJpaRepository userPassRepository;

    public UsageHistoryService(
            BenefitUsageLogJpaRepository usageLogRepository,
            UserPassJpaRepository userPassRepository) {
        this.usageLogRepository = usageLogRepository;
        this.userPassRepository = userPassRepository;
    }

    @Transactional(readOnly = true)
    public List<UsageHistoryItem> recentUsage(UUID accountId, int limit) {
        int size = Math.min(Math.max(limit, 1), MAX_LIMIT);
        return usageLogRepository.findRecentUsage(accountId, PageRequest.of(0, size));
    }

    @Transactional(readOnly = true)
    public List<UsageHistoryItem> usageForPass(UUID accountId, UUID userPassId) {
        if (userPassRepository.findByUserPassIdAndAccount_AccountId(userPassId, accountId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return usageLogRepository.findAllUsageByUserPassIdAndAccountId(userPassId, accountId);
    }
}
