package com.chiangraipass.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import com.chiangraipass.auth.entity.VisitorAccountEntity;
import com.chiangraipass.auth.model.EmailVisitorPrincipal;
import com.chiangraipass.auth.repository.VisitorAccountJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LoginAttemptServiceTest {

    @Autowired
    private VisitorAccountJpaRepository accounts;

    private Instant now;
    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        now = Instant.now();
        service = new LoginAttemptService(accounts, Clock.fixed(now, ZoneOffset.UTC));
    }

    private VisitorAccountEntity saveAccount(String email) {
        return accounts.saveAndFlush(
                VisitorAccountEntity.newEmailAccount(UUID.randomUUID(), email, "Tester", "hash", "v1"));
    }

    @Test
    void locksTheAccountAfterFiveFailures() {
        VisitorAccountEntity account = saveAccount("a@example.com");

        for (int i = 0; i < 4; i++) {
            service.recordFailure("a@example.com");
        }
        assertThat(account.isLocked(now)).isFalse();

        service.recordFailure("a@example.com");

        assertThat(account.isLocked(now)).isTrue();
        assertThat(account.getLockedUntil()).isEqualTo(now.plus(LoginAttemptService.LOCK_DURATION));
        assertThat(new EmailVisitorPrincipal(account.toAccount()).isAccountNonLocked()).isFalse();
    }

    @Test
    void emailMatchingIgnoresCaseAndSurroundingSpaces() {
        VisitorAccountEntity account = saveAccount("a@example.com");

        service.recordFailure("  A@EXAMPLE.com ");

        assertThat(account.getFailedLoginCount()).isEqualTo(1);
    }

    @Test
    void failuresAreCountedPerAccount() {
        VisitorAccountEntity first = saveAccount("a@example.com");
        VisitorAccountEntity second = saveAccount("b@example.com");

        service.recordFailure("a@example.com");
        service.recordFailure("a@example.com");

        assertThat(first.getFailedLoginCount()).isEqualTo(2);
        assertThat(second.getFailedLoginCount()).isZero();
    }

    @Test
    void successfulSignInClearsFailures() {
        VisitorAccountEntity account = saveAccount("a@example.com");
        service.recordFailure("a@example.com");
        service.recordFailure("a@example.com");

        service.recordSuccess(account.getAccountId());

        assertThat(account.getFailedLoginCount()).isZero();
        assertThat(account.getLockedUntil()).isNull();
    }

    @Test
    void unknownBlankAndNullInputsAreIgnoredWithoutError() {
        long existingAccounts = accounts.count();

        service.recordFailure("nobody@example.com");
        service.recordFailure("   ");
        service.recordFailure(null);
        service.recordSuccess(UUID.randomUUID());

        assertThat(accounts.count()).isEqualTo(existingAccounts);
    }

}