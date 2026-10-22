package com.chiangraipass.auth.entity;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VisitorAccountEntityTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Duration LOCK = Duration.ofMinutes(15);

    private VisitorAccountEntity emailAccount() {
        return VisitorAccountEntity.newEmailAccount(
                UUID.randomUUID(), "A@Example.com", "A", "hash", "v1");
    }

    @Test
    void newEmailAccountStartsUnverifiedAndUnlocked() {
        VisitorAccountEntity account = emailAccount();

        assertThat(account.isEmailVerified()).isFalse();
        assertThat(account.toAccount().emailVerified()).isFalse();
        assertThat(account.isLocked(NOW)).isFalse();
        assertThat(account.getFailedLoginCount()).isZero();
    }

    @Test
    void oauthAccountIsVerifiedBecauseProviderConfirmedTheEmail() {
        VisitorAccountEntity account = VisitorAccountEntity.newOAuthAccount(
                UUID.randomUUID(), "a@example.com", "A", "google", "sub-1", "v1");

        assertThat(account.isEmailVerified()).isTrue();
    }

    @Test
    void markEmailVerifiedKeepsTheFirstTimestamp() {
        VisitorAccountEntity account = emailAccount();
        account.markEmailVerified(NOW);
        account.markEmailVerified(NOW.plusSeconds(60));

        assertThat(account.isEmailVerified()).isTrue();
    }

    @Test
    void locksAfterTheMaximumNumberOfFailures() {
        VisitorAccountEntity account = emailAccount();
        for (int i = 0; i < 4; i++) {
            account.recordFailedLogin(NOW, 5, LOCK);
        }
        assertThat(account.isLocked(NOW)).isFalse();

        account.recordFailedLogin(NOW, 5, LOCK);

        assertThat(account.isLocked(NOW)).isTrue();
        assertThat(account.isLocked(NOW.plus(LOCK).minusSeconds(1))).isTrue();
        assertThat(account.isLocked(NOW.plus(LOCK))).isFalse();
        assertThat(account.toAccount().isLocked(NOW)).isTrue();
    }

    @Test
    void failureCounterRestartsAfterALockExpires() {
        VisitorAccountEntity account = emailAccount();
        for (int i = 0; i < 5; i++) {
            account.recordFailedLogin(NOW, 5, LOCK);
        }
        Instant later = NOW.plus(LOCK).plusSeconds(1);

        account.recordFailedLogin(later, 5, LOCK);

        assertThat(account.getFailedLoginCount()).isEqualTo(1);
        assertThat(account.isLocked(later)).isFalse();
    }

    @Test
    void clearLoginFailuresResetsCounterAndLock() {
        VisitorAccountEntity account = emailAccount();
        for (int i = 0; i < 5; i++) {
            account.recordFailedLogin(NOW, 5, LOCK);
        }

        account.clearLoginFailures();

        assertThat(account.isLocked(NOW)).isFalse();
        assertThat(account.getFailedLoginCount()).isZero();
    }
}