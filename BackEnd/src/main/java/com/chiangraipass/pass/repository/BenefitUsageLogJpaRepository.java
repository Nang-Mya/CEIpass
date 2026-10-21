package com.chiangraipass.pass.repository;

import com.chiangraipass.pass.entity.BenefitUsageLogEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BenefitUsageLogJpaRepository extends JpaRepository<BenefitUsageLogEntity, UUID> {
}
