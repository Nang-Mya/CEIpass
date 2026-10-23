package com.chiangraipass.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

import com.chiangraipass.auth.repository.VisitorAccountJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Counts failed password attempts per account and locks the account temporarily. */
@Service
public class LoginAttemptService {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(10);

    private final VisitorAccountJpaRepository accounts;
    private final Clock clock;

    @Autowired
    public LoginAttemptService(VisitorAccountJpaRepository accounts) {
        this(accounts, Clock.systemUTC());
    }

    LoginAttemptService(VisitorAccountJpaRepository accounts, Clock clock) {
        this.accounts = accounts;
        this.clock = clock;
    }

    /** Unknown emails are ignored so that behaviour does not reveal which accounts exist. */
    @Transactional
    public void recordFailure(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        accounts.findActiveForUpdate(VisitorAccountService.normalizeEmail(email))
                .ifPresent(account ->
                        account.recordFailedLogin(clock.instant(), MAX_FAILED_ATTEMPTS, LOCK_DURATION));
    }

    @Transactional
    public void recordSuccess(UUID accountId) {
        accounts.findById(accountId).ifPresent(account -> {
            if (account.getFailedLoginCount() > 0 || account.getLockedUntil() != null) {
                account.clearLoginFailures();
            }
        });
    }

}