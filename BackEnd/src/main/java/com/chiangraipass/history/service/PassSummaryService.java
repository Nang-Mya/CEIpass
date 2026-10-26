package com.chiangraipass.history.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.chiangraipass.history.dto.PassSummaryResponse;
import com.chiangraipass.history.dto.PassSummaryResponse.BenefitQuota;
import com.chiangraipass.history.dto.PassSummaryResponse.PassInfo;
import com.chiangraipass.history.dto.UsageHistoryItem;
import com.chiangraipass.pass.entity.UserPassEntity;
import com.chiangraipass.pass.repository.BenefitJpaRepository;
import com.chiangraipass.pass.repository.BenefitUsageLogJpaRepository;
import com.chiangraipass.pass.repository.UserPassJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PassSummaryService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Bangkok");
    private static final int RECENT_LIMIT = 5;

    private final UserPassJpaRepository userPasses;
    private final BenefitJpaRepository benefits;
    private final BenefitUsageLogJpaRepository usageLogs;

    public PassSummaryService(
            UserPassJpaRepository userPasses,
            BenefitJpaRepository benefits,
            BenefitUsageLogJpaRepository usageLogs) {
        this.userPasses = userPasses;
        this.benefits = benefits;
        this.usageLogs = usageLogs;
    }

    @Transactional(readOnly = true)
    public PassSummaryResponse summary(UUID accountId) {
        List<UsageHistoryItem> recent = usageLogs.findRecentUsage(accountId, PageRequest.of(0, RECENT_LIMIT));
        LocalDate today = LocalDate.now(ZONE);
        List<UserPassEntity> all = userPasses.findByAccount_AccountIdOrderByPurchaseDatetimeDesc(accountId);
        Optional<UserPassEntity> current = pickCurrent(all, today);
        if (current.isEmpty()) {
            return new PassSummaryResponse(null, List.of(), recent);
        }

        UserPassEntity userPass = current.get();
        Instant endsAt = userPass.getEndDate() == null
                ? null
                : userPass.getEndDate().plusDays(1).atStartOfDay(ZONE).toInstant();
        PassInfo info = new PassInfo(
                userPass.getPass().getDisplayName(),
                effectiveStatus(userPass, today),
                userPass.getStartDate(),
                userPass.getEndDate(),
                endsAt);

        Map<String, Long> used = new HashMap<>();
        for (Object[] row : usageLogs.countSuccessfulUsageByBenefit(userPass.getUserPassId())) {
            used.put((String) row[0], ((Number) row[1]).longValue());
        }
        List<BenefitQuota> quotas = benefits
                .findByPass_PassIdOrderByBenefitIdAsc(userPass.getPass().getPassId())
                .stream()
                .map(b -> new BenefitQuota(
                        b.getPartner().getPublicName(),
                        b.getBenefitDetail(),
                        used.getOrDefault(b.getBenefitId(), 0L),
                        b.getUsageRuleLimit()))
                .toList();

        return new PassSummaryResponse(info, quotas, recent);
    }

    private Optional<UserPassEntity> pickCurrent(List<UserPassEntity> all, LocalDate today) {
        for (String wanted : List.of("ACTIVE", "UNACTIVATED")) {
            for (UserPassEntity candidate : all) {
                if (wanted.equals(effectiveStatus(candidate, today))) {
                    return Optional.of(candidate);
                }
            }
        }
        return all.stream().findFirst();
    }

    private String effectiveStatus(UserPassEntity userPass, LocalDate today) {
        if ("ACTIVE".equals(userPass.getStatus())
                && userPass.getEndDate() != null
                && today.isAfter(userPass.getEndDate())) {
            return "EXPIRED";
        }
        return userPass.getStatus();
    }
}
