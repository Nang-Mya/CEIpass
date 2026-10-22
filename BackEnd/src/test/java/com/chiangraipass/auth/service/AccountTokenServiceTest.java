package com.chiangraipass.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import com.chiangraipass.auth.model.AccountTokenPurpose;
import com.chiangraipass.auth.repository.AccountTokenJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AccountTokenServiceTest {

    private static final Duration HOUR = Duration.ofHours(1);

    @Autowired
    private AccountTokenJpaRepository repository;

    private MutableClock clock;
    private AccountTokenService service;
    private UUID accountId;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        service = new AccountTokenService(repository, clock);
        accountId = UUID.randomUUID();
    }

    @Test
    void issuedTokenIsRedeemableOnceAndReturnsOwner() {
        String token = service.issue(accountId, AccountTokenPurpose.VERIFY_EMAIL, HOUR);

        assertThat(service.consume(token, AccountTokenPurpose.VERIFY_EMAIL)).contains(accountId);
        assertThat(service.consume(token, AccountTokenPurpose.VERIFY_EMAIL)).isEmpty();
    }

    @Test
    void onlyTheHashIsStored() {
        String token = service.issue(accountId, AccountTokenPurpose.PASSWORD_RESET, HOUR);

        assertThat(repository.findAll()).hasSize(1);
        assertThat(repository.findAll().get(0).getTokenHash())
                .isEqualTo(AccountTokenService.hash(token))
                .isNotEqualTo(token)
                .hasSize(64);
    }

    @Test
    void tokenCannotBeUsedForAnotherPurpose() {
        String token = service.issue(accountId, AccountTokenPurpose.VERIFY_EMAIL, HOUR);

        assertThat(service.consume(token, AccountTokenPurpose.PASSWORD_RESET)).isEmpty();
        assertThat(service.consume(token, AccountTokenPurpose.VERIFY_EMAIL)).contains(accountId);
    }

    @Test
    void expiredTokenIsRejected() {
        String token = service.issue(accountId, AccountTokenPurpose.PASSWORD_RESET, HOUR);
        clock.advance(HOUR.plusSeconds(1));

        assertThat(service.consume(token, AccountTokenPurpose.PASSWORD_RESET)).isEmpty();
    }

    @Test
    void issuingANewTokenInvalidatesThePreviousOneOfTheSamePurpose() {
        String first = service.issue(accountId, AccountTokenPurpose.PASSWORD_RESET, HOUR);
        String second = service.issue(accountId, AccountTokenPurpose.PASSWORD_RESET, HOUR);

        assertThat(service.consume(first, AccountTokenPurpose.PASSWORD_RESET)).isEmpty();
        assertThat(service.consume(second, AccountTokenPurpose.PASSWORD_RESET)).contains(accountId);
    }

    @Test
    void differentPurposesDoNotInvalidateEachOther() {
        String verify = service.issue(accountId, AccountTokenPurpose.VERIFY_EMAIL, HOUR);
        service.issue(accountId, AccountTokenPurpose.PASSWORD_RESET, HOUR);

        assertThat(service.consume(verify, AccountTokenPurpose.VERIFY_EMAIL)).contains(accountId);
    }

    @Test
    void unknownBlankAndOversizedTokensAreRejected() {
        assertThat(service.consume("not-a-real-token", AccountTokenPurpose.VERIFY_EMAIL)).isEmpty();
        assertThat(service.consume("  ", AccountTokenPurpose.VERIFY_EMAIL)).isEmpty();
        assertThat(service.consume(null, AccountTokenPurpose.VERIFY_EMAIL)).isEmpty();
        assertThat(service.consume("x".repeat(500), AccountTokenPurpose.VERIFY_EMAIL)).isEmpty();
    }

    @Test
    void tokensAreUnique() {
        String a = service.issue(accountId, AccountTokenPurpose.VERIFY_EMAIL, HOUR);
        String b = service.issue(UUID.randomUUID(), AccountTokenPurpose.VERIFY_EMAIL, HOUR);

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void nonPositiveLifetimeIsRejected() {
        assertThatThrownBy(() -> service.issue(accountId, AccountTokenPurpose.VERIFY_EMAIL, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void purgeRemovesOnlyTokensExpiredBeyondRetention() {
        service.issue(accountId, AccountTokenPurpose.VERIFY_EMAIL, HOUR);
        clock.advance(Duration.ofDays(10));
        String fresh = service.issue(accountId, AccountTokenPurpose.PASSWORD_RESET, HOUR);

        assertThat(service.purgeExpired(Duration.ofDays(7))).isEqualTo(1);
        assertThat(service.consume(fresh, AccountTokenPurpose.PASSWORD_RESET)).contains(accountId);
    }

    @Test
    void reportsWhetherATokenWasIssuedRecently() {
        service.issue(accountId, AccountTokenPurpose.VERIFY_EMAIL, HOUR);

        assertThat(service.issuedRecently(accountId, AccountTokenPurpose.VERIFY_EMAIL, Duration.ofMinutes(1))).isTrue();
        assertThat(service.issuedRecently(accountId, AccountTokenPurpose.PASSWORD_RESET, Duration.ofMinutes(1))).isFalse();
        assertThat(service.issuedRecently(UUID.randomUUID(), AccountTokenPurpose.VERIFY_EMAIL, Duration.ofMinutes(1))).isFalse();

        clock.advance(Duration.ofMinutes(2));

        assertThat(service.issuedRecently(accountId, AccountTokenPurpose.VERIFY_EMAIL, Duration.ofMinutes(1))).isFalse();
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}