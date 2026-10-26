package com.chiangraipass.auth.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.chiangraipass.auth.entity.AccountTokenEntity;
import com.chiangraipass.auth.model.AccountTokenPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountTokenJpaRepository extends JpaRepository<AccountTokenEntity, UUID> {

    Optional<AccountTokenEntity> findByTokenHash(String tokenHash);

    /**
     * Atomically marks a valid token as used. Returns 1 for exactly one caller; concurrent
     * or repeated attempts get 0, so a token cannot be redeemed twice.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE AccountTokenEntity t
            SET t.consumedAt = :now
            WHERE t.tokenHash = :hash
              AND t.purpose = :purpose
              AND t.consumedAt IS NULL
              AND t.expiresAt > :now
            """)
    int consume(@Param("hash") String hash,
            @Param("purpose") AccountTokenPurpose purpose,
            @Param("now") Instant now);

    /** Invalidates outstanding tokens of one purpose for an account (a newer one replaces them). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE AccountTokenEntity t
            SET t.consumedAt = :now
            WHERE t.accountId = :accountId
              AND t.purpose = :purpose
              AND t.consumedAt IS NULL
            """)
    int invalidateOutstanding(@Param("accountId") UUID accountId,
            @Param("purpose") AccountTokenPurpose purpose,
            @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM AccountTokenEntity t WHERE t.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);

}