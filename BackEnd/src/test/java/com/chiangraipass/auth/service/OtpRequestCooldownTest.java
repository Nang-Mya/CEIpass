package com.chiangraipass.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OtpRequestCooldownTest {

    private static final class SteppingClock extends Clock {
        private Instant now = Instant.parse("2026-10-26T08:00:00Z");

        void advanceMillis(long millis) {
            now = now.plusMillis(millis);
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

    private final SteppingClock clock = new SteppingClock();
    private final OtpRequestCooldown cooldown = new OtpRequestCooldown(clock);

    @Test
    void theFirstRequestIsAllowedAndASecondOneHasToWait() {
        assertThat(cooldown.tryStart("visitor@example.com")).isZero();
        assertThat(cooldown.tryStart("visitor@example.com")).isEqualTo(60);
    }

    @Test
    void theWaitCountsDownAndRoundsUp() {
        cooldown.tryStart("visitor@example.com");
        clock.advanceMillis(20_500);

        assertThat(cooldown.tryStart("visitor@example.com")).isEqualTo(40);
    }

    @Test
    void aRefusedRequestDoesNotRestartTheCooldown() {
        cooldown.tryStart("visitor@example.com");
        clock.advanceMillis(30_000);
        cooldown.tryStart("visitor@example.com");
        clock.advanceMillis(30_000);

        assertThat(cooldown.tryStart("visitor@example.com")).isZero();
    }

    @Test
    void anotherCaseOrSpacingOfTheSameEmailSharesTheCooldown() {
        cooldown.tryStart("visitor@example.com");

        assertThat(cooldown.tryStart("  Visitor@Example.COM ")).isEqualTo(60);
    }

    @Test
    void otherEmailsAreNotAffected() {
        cooldown.tryStart("visitor@example.com");

        assertThat(cooldown.tryStart("someone.else@example.com")).isZero();
    }

    @Test
    void anEmailThatWasNeverRegisteredIsTreatedTheSameAsOneThatWas() {
        assertThat(cooldown.tryStart("nobody@example.com")).isZero();
        assertThat(cooldown.tryStart("nobody@example.com")).isEqualTo(60);
    }
}