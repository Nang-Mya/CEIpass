package com.chiangraipass.pass.repository;

import com.chiangraipass.pass.entity.UserPassEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserPassJpaRepository extends JpaRepository<UserPassEntity, UUID> {

    @Query("""
            SELECT up FROM UserPassEntity up
            JOIN FETCH up.pass
            WHERE up.account.accountId = :accountId
            ORDER BY up.purchaseDatetime DESC
            """)
    List<UserPassEntity> findByAccountIdWithPass(@Param("accountId") UUID accountId);

    List<UserPassEntity> findByAccount_AccountIdOrderByPurchaseDatetimeDesc(UUID accountId);
}
