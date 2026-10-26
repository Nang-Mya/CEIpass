package com.chiangraipass.pass.repository;

import com.chiangraipass.pass.entity.UserPassEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPassJpaRepository extends JpaRepository<UserPassEntity, UUID> {

    List<UserPassEntity> findByAccount_AccountIdOrderByPurchaseDatetimeDesc(UUID accountId);
}
