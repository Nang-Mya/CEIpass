package com.chiangraipass.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Allows one password-reset code request per email address per cooldown, whether or not the
 * address belongs to an account, so a "please wait" answer reveals nothing about which emails
 * have accounts. It is kept in memory, so it applies per server instance; the per-account check
 * in the database backs it up.
 */
public class OtpRequestCooldown {

    public static final Duration COOLDOWN = Duration.ofMinutes(1);
    private static final int PURGE_THRESHOLD = 10_000;

    private final Clock clock;
    private final ConcurrentMap<String, Instant> nextAllowed = new ConcurrentHashMap<>();

    public OtpRequestCooldown() {
        this(Clock.systemUTC());
    }

    OtpRequestCooldown(Clock clock) {
        this.clock = clock;
    }

    /**
     * Starts the cooldown for this email if none is running and returns 0. If one is running it
     * returns the whole seconds left and does not restart it.
     */
    public long tryStart(String email) {
        String key = email.trim().toLowerCase(Locale.ROOT);
        Instant now = clock.instant();
        long[] secondsLeft = {0};
        nextAllowed.compute(key, (ignored, until) -> {
            if (until != null && until.isAfter(now)) {
                secondsLeft[0] = Math.max(1, (Duration.between(now, until).toMillis() + 999) / 1000);
                return until;
            }
            return now.plus(COOLDOWN);
        });
        if (nextAllowed.size() > PURGE_THRESHOLD) {
            nextAllowed.values().removeIf(until -> !until.isAfter(now));
        }
        return secondsLeft[0];
    }
}