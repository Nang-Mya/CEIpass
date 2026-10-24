package com.chiangraipass.auth.repository;

import java.util.Optional;
import java.util.UUID;

import com.chiangraipass.auth.entity.VisitorAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

public interface VisitorAccountJpaRepository extends JpaRepository<VisitorAccountEntity, UUID> {

    Optional<VisitorAccountEntity> findByNormalizedEmailAndAccountStatus(String normalizedEmail, String accountStatus);

    @Query("""
            SELECT account
            FROM VisitorAccountEntity account
            JOIN account.identities identity
            WHERE identity.provider = :provider
              AND identity.providerSubject = :subject
              AND account.accountStatus = 'ACTIVE'
            """)
    Optional<VisitorAccountEntity> findActiveByProviderIdentity(
            @Param("provider") String provider,
            @Param("subject") String subject);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT account
            FROM VisitorAccountEntity account
            WHERE account.normalizedEmail = :email
              AND account.accountStatus = 'ACTIVE'
            """)
    Optional<VisitorAccountEntity> findActiveForUpdate(@Param("email") String email);

    @Query("""
            SELECT account.securityToken
            FROM VisitorAccountEntity account
            WHERE account.accountId = :accountId
              AND account.accountStatus = 'ACTIVE'
            """)
    Optional<String> findActiveSecurityToken(@Param("accountId") UUID accountId);
}
