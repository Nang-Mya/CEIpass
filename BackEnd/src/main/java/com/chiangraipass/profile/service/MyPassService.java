package com.chiangraipass.profile.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.chiangraipass.pass.entity.UserPassEntity;
import com.chiangraipass.profile.dto.MyPassResponse.BenefitUsage;
import com.chiangraipass.profile.dto.MyPassResponse.PassDetails;
import com.chiangraipass.profile.repository.ProfilePassRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MyPassService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Bangkok");

    private final ProfilePassRepository passes;

    public MyPassService(ProfilePassRepository passes) {
        this.passes = passes;
    }

    /** The visitor's active pass if there is one, otherwise their most recent pass. */
    @Transactional(readOnly = true)
    public Optional<PassDetails> currentPass(UUID accountId) {
        List<UserPassEntity> owned = passes.findByAccount(accountId);
        if (owned.isEmpty()) {
            return Optional.empty();
        }
        LocalDate today = LocalDate.now(ZONE);
        UserPassEntity chosen = owned.stream()
                .filter(pass -> isActive(pass, today))
                .findFirst()
                .orElse(owned.get(0));

        Map<String, Integer> usedByBenefit = new HashMap<>();
        for (Object[] row : passes.countUsedBenefits(chosen.getUserPassId())) {
            usedByBenefit.put((String) row[0], ((Number) row[1]).intValue());
        }
        List<BenefitUsage> benefits = passes.findBenefitsOfPass(chosen.getPass().getPassId()).stream()
                .map(benefit -> new BenefitUsage(
                        benefit.getPartner().getPublicName(),
                        benefit.getBenefitDetail(),
                        Math.min(usedByBenefit.getOrDefault(benefit.getBenefitId(), 0), benefit.getUsageRuleLimit()),
                        benefit.getUsageRuleLimit()))
                .toList();

        return Optional.of(new PassDetails(
                chosen.getPass().getDisplayName(),
                isActive(chosen, today) ? "ACTIVE" : "NOT_ACTIVE",
                chosen.getStartDate(),
                chosen.getEndDate(),
                benefits));
    }

    private static boolean isActive(UserPassEntity pass, LocalDate today) {
        return "ACTIVE".equalsIgnoreCase(pass.getStatus())
                && (pass.getStartDate() == null || !today.isBefore(pass.getStartDate()))
                && (pass.getEndDate() == null || !today.isAfter(pass.getEndDate()));
    }
}