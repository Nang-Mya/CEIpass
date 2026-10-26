package com.chiangraipass.profile.repository;

import java.util.List;
import java.util.UUID;

import com.chiangraipass.pass.entity.BenefitEntity;
import com.chiangraipass.pass.entity.UserPassEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Read-only queries for the profile page. */
public interface ProfilePassRepository extends JpaRepository<UserPassEntity, UUID> {

    @Query("""
            select up from UserPassEntity up
            join fetch up.pass
            where up.account.accountId = :accountId
            order by up.purchaseDatetime desc
            """)
    List<UserPassEntity> findByAccount(@Param("accountId") UUID accountId);

    @Query("""
            select b from BenefitEntity b
            join fetch b.partner
            where b.pass.passId = :passId
            order by b.benefitId
            """)
    List<BenefitEntity> findBenefitsOfPass(@Param("passId") String passId);

    @Query("""
            select l.benefit.benefitId, count(l) from BenefitUsageLogEntity l
            where l.userPass.userPassId = :userPassId
              and upper(l.validationStatus) not in ('REJECTED', 'FAILED', 'INVALID')
            group by l.benefit.benefitId
            """)
    List<Object[]> countUsedBenefits(@Param("userPassId") UUID userPassId);
}