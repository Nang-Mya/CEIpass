package com.chiangraipass.api.auth;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import com.chiangraipass.persistence.entity.VisitorAccountEntity;
import com.chiangraipass.persistence.repository.VisitorAccountJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class VisitorAccountRepository {

    private static final String ACTIVE = "ACTIVE";

    private final VisitorAccountJpaRepository accounts;

    public VisitorAccountRepository(VisitorAccountJpaRepository accounts) {
        this.accounts = accounts;
    }

    public Optional<VisitorAccount> findByEmail(String email) {
        return accounts.findByNormalizedEmailAndAccountStatus(normalizeEmail(email), ACTIVE)
                .map(VisitorAccountEntity::toAccount);
    }

    public Optional<VisitorAccount> findByExternalIdentity(String provider, String subject) {
        return accounts.findActiveByProviderIdentity(provider, subject)
                .map(VisitorAccountEntity::toAccount);
    }

    @Transactional
    public void createEmailAccount(
            UUID accountId,
            String email,
            String displayName,
            String passwordHash,
            String privacyNoticeVersion) {
        accounts.save(VisitorAccountEntity.newEmailAccount(
                accountId,
                email,
                displayName,
                passwordHash,
                privacyNoticeVersion));
    }

    @Transactional
    public VisitorAccount createOAuthAccount(
            UUID accountId,
            String email,
            String displayName,
            String provider,
            String subject,
            String privacyNoticeVersion) {
        VisitorAccountEntity account = accounts.save(VisitorAccountEntity.newOAuthAccount(
                accountId,
                email,
                displayName,
                provider,
                subject,
                privacyNoticeVersion));
        return account.toAccount();
    }

    public static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
