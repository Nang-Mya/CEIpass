package com.chiangraipass.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import com.chiangraipass.persistence.entity.VisitorAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
