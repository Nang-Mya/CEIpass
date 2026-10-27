package com.chiangraipass.pass.repository;

import java.util.List;

import com.chiangraipass.pass.entity.BenefitEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BenefitJpaRepository extends JpaRepository<BenefitEntity, String> {

    List<BenefitEntity> findByPass_PassIdOrderByBenefitIdAsc(String passId);
}
