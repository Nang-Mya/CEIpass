package com.chiangraipass.pass.repository;

import com.chiangraipass.history.dto.UsageHistoryItem;
import com.chiangraipass.pass.entity.BenefitUsageLogEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BenefitUsageLogJpaRepository extends JpaRepository<BenefitUsageLogEntity, UUID> {

    @Query("""
            select new com.chiangraipass.history.dto.UsageHistoryItem(
                a.publicName, p.displayName, l.checkinDatetime, l.validationStatus)
            from BenefitUsageLogEntity l
            join l.userPass up
            join up.pass p
            join l.partner a
            where up.account.accountId = :accountId
            order by l.checkinDatetime desc
            """)
    List<UsageHistoryItem> findRecentUsage(@Param("accountId") UUID accountId, Pageable pageable);

    @Query("""
            select new com.chiangraipass.history.dto.UsageHistoryItem(
                a.publicName, p.displayName, l.checkinDatetime, l.validationStatus)
            from BenefitUsageLogEntity l
            join l.userPass up
            join up.pass p
            join l.partner a
            where up.userPassId = :userPassId
              and up.account.accountId = :accountId
            order by l.checkinDatetime desc
            """)
    List<UsageHistoryItem> findRecentUsageByUserPassIdAndAccountId(
            @Param("userPassId") UUID userPassId,
            @Param("accountId") UUID accountId,
            Pageable pageable);

    @Query("""
            select new com.chiangraipass.history.dto.UsageHistoryItem(
                a.publicName, p.displayName, l.checkinDatetime, l.validationStatus)
            from BenefitUsageLogEntity l
            join l.userPass up
            join up.pass p
            join l.partner a
            where up.userPassId = :userPassId
              and up.account.accountId = :accountId
            order by l.checkinDatetime desc
            """)
    List<UsageHistoryItem> findAllUsageByUserPassIdAndAccountId(
            @Param("userPassId") UUID userPassId,
            @Param("accountId") UUID accountId);

    @Query("""
            select l.benefit.benefitId, count(l)
            from BenefitUsageLogEntity l
            where l.userPass.userPassId = :userPassId
              and l.validationStatus = 'VALID_SUCCESS'
            group by l.benefit.benefitId
            """)
    List<Object[]> countSuccessfulUsageByBenefit(@Param("userPassId") UUID userPassId);
}
