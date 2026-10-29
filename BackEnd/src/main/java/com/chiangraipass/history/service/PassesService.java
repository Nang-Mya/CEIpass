package com.chiangraipass.history.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.chiangraipass.history.dto.PassCardResponse;
import com.chiangraipass.history.dto.PassCardResponse.BenefitQuota;
import com.chiangraipass.history.dto.UsageHistoryItem;
import com.chiangraipass.pass.entity.UserPassEntity;
import com.chiangraipass.pass.repository.BenefitJpaRepository;
import com.chiangraipass.pass.repository.BenefitUsageLogJpaRepository;
import com.chiangraipass.pass.repository.UserPassJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PassesService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Bangkok");
    private static final int RECENT_LIMIT = 5;

    private final UserPassJpaRepository userPasses;
    private final BenefitJpaRepository benefits;
    private final BenefitUsageLogJpaRepository usageLogs;

    public PassesService(
            UserPassJpaRepository userPasses,
            BenefitJpaRepository benefits,
            BenefitUsageLogJpaRepository usageLogs) {
        this.userPasses = userPasses;
        this.benefits = benefits;
        this.usageLogs = usageLogs;
    }

    @Transactional(readOnly = true)
    public List<PassCardResponse> passes(UUID accountId) {
        LocalDate today = LocalDate.now(ZONE);
        return userPasses.findByAccount_AccountIdOrderByPurchaseDatetimeDesc(accountId).stream()
                .sorted(Comparator.comparing(UserPassEntity::getPurchaseDatetime))
                .map(userPass -> passCard(userPass, accountId, today))
                .toList();
    }

    private PassCardResponse passCard(UserPassEntity userPass, UUID accountId, LocalDate today) {
        Map<String, Long> used = new HashMap<>();
        for (Object[] row : usageLogs.countSuccessfulUsageByBenefit(userPass.getUserPassId())) {
            used.put((String) row[0], ((Number) row[1]).longValue());
        }

        List<BenefitQuota> quotas = benefits
                .findByPass_PassIdOrderByBenefitIdAsc(userPass.getPass().getPassId())
                .stream()
                .map(benefit -> new BenefitQuota(
                        benefit.getPartner().getPublicName(),
                        benefit.getBenefitDetail(),
                        used.getOrDefault(benefit.getBenefitId(), 0L),
                        benefit.getUsageRuleLimit()))
                .toList();
        List<UsageHistoryItem> recentUsage =
                usageLogs.findRecentUsageByUserPassIdAndAccountId(
                        userPass.getUserPassId(), accountId, PageRequest.of(0, RECENT_LIMIT));
        Instant startsAt = userPass.getStartDate() == null
                ? null
                : userPass.getStartDate().atStartOfDay(ZONE).toInstant();
        Instant endsAt = userPass.getEndDate() == null
                ? null
                : userPass.getEndDate().atTime(23, 59, 59).atZone(ZONE).toInstant();

        return new PassCardResponse(
                userPass.getUserPassId(),
                userPass.getPass().getDisplayName(),
                userPass.getHolderName(),
                userPass.getPurchaseDatetime(),
                userPass.getStartDate(),
                userPass.getEndDate(),
                PassSummaryService.displayStatus(userPass, today),
                startsAt,
                endsAt,
                quotas,
                recentUsage);
    }
}
