package com.chiangraipass.auth.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.chiangraipass.auth.entity.PasswordResetOtpEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetOtpRepository
        extends JpaRepository<PasswordResetOtpEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT otp
            FROM PasswordResetOtpEntity otp
            WHERE otp.accountId = :accountId
              AND otp.consumedAt IS NULL
              AND otp.expiresAt > :now
            ORDER BY otp.createdAt DESC
            """)
    List<PasswordResetOtpEntity> findActiveForUpdate(
            @Param("accountId") UUID accountId,
            @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE PasswordResetOtpEntity otp
            SET otp.consumedAt = :now
            WHERE otp.accountId = :accountId
              AND otp.consumedAt IS NULL
            """)
    int invalidateOutstanding(
            @Param("accountId") UUID accountId,
            @Param("now") Instant now);

    boolean existsByAccountIdAndCreatedAtAfter(
            UUID accountId,
            Instant createdAfter);
}