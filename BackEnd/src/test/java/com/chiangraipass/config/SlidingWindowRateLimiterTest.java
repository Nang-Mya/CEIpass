package com.chiangraipass.config;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SlidingWindowRateLimiterTest {

    private MutableClock clock;
    private SlidingWindowRateLimiter limiter;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        limiter = new SlidingWindowRateLimiter(3, Duration.ofSeconds(60), clock);
    }

    @Test
    void allowsUpToTheLimitThenBlocks() {
        for (int i = 0; i < 3; i++) {
            assertThat(limiter.tryAcquire("a").allowed()).isTrue();
        }
        SlidingWindowRateLimiter.Decision blocked = limiter.tryAcquire("a");

        assertThat(blocked.allowed()).isFalse();
        assertThat(blocked.retryAfterSeconds()).isEqualTo(60);
    }

    @Test
    void keysAreIndependent() {
        for (int i = 0; i < 3; i++) {
            limiter.tryAcquire("a");
        }

        assertThat(limiter.tryAcquire("b").allowed()).isTrue();
    }

    @Test
    void retryAfterShrinksAndRequestsAreAllowedOnceTheWindowPasses() {
        for (int i = 0; i < 3; i++) {
            limiter.tryAcquire("a");
        }
        clock.advance(Duration.ofSeconds(30));
        assertThat(limiter.tryAcquire("a").retryAfterSeconds()).isEqualTo(30);

        clock.advance(Duration.ofSeconds(30));
        assertThat(limiter.tryAcquire("a").allowed()).isTrue();
    }

    @Test
    void blockedRequestsAreNotCounted() {
        for (int i = 0; i < 3; i++) {
            limiter.tryAcquire("a");
        }
        for (int i = 0; i < 50; i++) {
            limiter.tryAcquire("a");
        }
        clock.advance(Duration.ofSeconds(60));

        assertThat(limiter.tryAcquire("a").allowed()).isTrue();
    }

    @Test
    void expiredKeysAreCleanedUp() {
        for (int i = 0; i < 500; i++) {
            limiter.tryAcquire("ip-" + i);
        }
        clock.advance(Duration.ofSeconds(61));
        for (int i = 0; i < 600; i++) {
            limiter.tryAcquire("fresh");
        }

        assertThat(limiter.trackedKeys()).isLessThan(10);
    }

    @Test
    void invalidConfigurationIsRejected() {
        assertThatThrownBy(() -> new SlidingWindowRateLimiter(0, Duration.ofSeconds(1), clock))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SlidingWindowRateLimiter(1, Duration.ZERO, clock))
                .isInstanceOf(IllegalArgumentException.class);
    }
}