package com.chiangraipass.auth.model;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailVisitorPrincipalTest {

    private EmailVisitorPrincipal principal(Instant lockedUntil) {
        return new EmailVisitorPrincipal(
                new VisitorAccount(UUID.randomUUID(), "a@example.com", "A", "hash", true, lockedUntil));
    }

    @Test
    void accountWithFutureLockIsLocked() {
        assertThat(principal(Instant.now().plus(Duration.ofMinutes(5))).isAccountNonLocked()).isFalse();
    }

    @Test
    void expiredOrMissingLockMeansUnlocked() {
        assertThat(principal(Instant.now().minusSeconds(1)).isAccountNonLocked()).isTrue();
        assertThat(principal(null).isAccountNonLocked()).isTrue();
    }
}