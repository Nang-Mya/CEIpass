package com.chiangraipass.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import com.chiangraipass.auth.repository.PasswordResetOtpRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PasswordResetOtpServiceTest {

    @Autowired
    private PasswordResetOtpRepository repository;

    private MutableClock clock;
    private PasswordResetOtpService service;
    private UUID accountId;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        service = new PasswordResetOtpService(repository, new BCryptPasswordEncoder(4), clock);
        accountId = UUID.randomUUID();
    }

    private String wrongCodeFor(String code) {
        return code.equals("000000") ? "111111" : "000000";
    }

    @Test
    void issuesASixDigitCodeAndStoresOnlyAHash() {
        String code = service.issue(accountId);

        assertThat(code).matches("\\d{6}");
        assertThat(repository.findAll()).hasSize(1);
        assertThat(repository.findAll().get(0).getOtpHash()).isNotEqualTo(code).startsWith("$2");
    }

    @Test
    void aCorrectCodeWorksExactlyOnce() {
        String code = service.issue(accountId);

        assertThat(service.verify(accountId, code)).isTrue();
        assertThat(service.verify(accountId, code)).isFalse();
    }

    @Test
    void aWrongCodeIsRejected() {
        String code = service.issue(accountId);

        assertThat(service.verify(accountId, wrongCodeFor(code))).isFalse();
        assertThat(service.verify(accountId, code)).isTrue();
    }

    @Test
    void anExpiredCodeIsRejected() {
        String code = service.issue(accountId);
        clock.advance(Duration.ofMinutes(10).plusSeconds(1));

        assertThat(service.verify(accountId, code)).isFalse();
    }

    @Test
    void theCodeDiesAfterFiveWrongGuesses() {
        String code = service.issue(accountId);
        for (int i = 0; i < PasswordResetOtpService.MAX_FAILED_ATTEMPTS; i++) {
            assertThat(service.verify(accountId, wrongCodeFor(code))).isFalse();
        }

        assertThat(service.verify(accountId, code)).isFalse();
    }

    @Test
    void fourWrongGuessesStillAllowTheCorrectCode() {
        String code = service.issue(accountId);
        for (int i = 0; i < PasswordResetOtpService.MAX_FAILED_ATTEMPTS - 1; i++) {
            service.verify(accountId, wrongCodeFor(code));
        }

        assertThat(service.verify(accountId, code)).isTrue();
    }

    @Test
    void aNewCodeInvalidatesTheOldOne() {
        String first = service.issue(accountId);
        String second = service.issue(accountId);

        if (!first.equals(second)) {
            assertThat(service.verify(accountId, first)).isFalse();
        }
        assertThat(service.verify(accountId, second)).isTrue();
    }

    @Test
    void malformedInputIsRejectedWithoutUsingUpAttempts() {
        String code = service.issue(accountId);
        for (int i = 0; i < 20; i++) {
            assertThat(service.verify(accountId, "12345")).isFalse();
            assertThat(service.verify(accountId, "abcdef")).isFalse();
            assertThat(service.verify(accountId, null)).isFalse();
        }

        assertThat(service.verify(accountId, code)).isTrue();
    }

    @Test
    void anAccountWithoutACodeCannotVerify() {
        assertThat(service.verify(UUID.randomUUID(), "123456")).isFalse();
    }

    @Test
    void reportsWhetherACodeWasIssuedRecently() {
        service.issue(accountId);

        assertThat(service.issuedRecently(accountId, Duration.ofMinutes(1))).isTrue();
        assertThat(service.issuedRecently(UUID.randomUUID(), Duration.ofMinutes(1))).isFalse();

        clock.advance(Duration.ofMinutes(2));

        assertThat(service.issuedRecently(accountId, Duration.ofMinutes(1))).isFalse();
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