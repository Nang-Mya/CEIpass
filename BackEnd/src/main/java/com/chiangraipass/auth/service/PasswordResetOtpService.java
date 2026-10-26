package com.chiangraipass.auth.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.chiangraipass.auth.entity.PasswordResetOtpEntity;
import com.chiangraipass.auth.repository.PasswordResetOtpRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Issues and checks 6-digit password-reset codes. */
@Service
public class PasswordResetOtpService {

    static final Duration LIFETIME = Duration.ofMinutes(10);
    static final int MAX_FAILED_ATTEMPTS = 5;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final PasswordResetOtpRepository otps;
    private final PasswordEncoder encoder;
    private final Clock clock;

    @Autowired
    public PasswordResetOtpService(PasswordResetOtpRepository otps, PasswordEncoder encoder) {
        this(otps, encoder, Clock.systemUTC());
    }

    PasswordResetOtpService(PasswordResetOtpRepository otps, PasswordEncoder encoder, Clock clock) {
        this.otps = otps;
        this.encoder = encoder;
        this.clock = clock;
    }

    /** Creates a new code and invalidates earlier ones. Returns the plain code, to be emailed. */
    @Transactional
    public String issue(UUID accountId) {
        Instant now = clock.instant();
        otps.invalidateOutstanding(accountId, now);
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        otps.save(new PasswordResetOtpEntity(accountId, encoder.encode(code), now, now.plus(LIFETIME)));
        return code;
    }

    @Transactional(readOnly = true)
    public boolean issuedRecently(UUID accountId, Duration within) {
        return otps.existsByAccountIdAndCreatedAtAfter(accountId, clock.instant().minus(within));
    }

    /**
     * Checks a submitted code against the newest active one. A wrong guess counts against that
     * code; after {@value #MAX_FAILED_ATTEMPTS} wrong guesses it can no longer be used. A correct
     * code works once.
     */
    @Transactional
    public boolean verify(UUID accountId, String submitted) {
        if (submitted == null || !submitted.matches("\\d{6}")) {
            return false;
        }
        Instant now = clock.instant();
        List<PasswordResetOtpEntity> active = otps.findActiveForUpdate(accountId, now);
        if (active.isEmpty()) {
            return false;
        }
        PasswordResetOtpEntity otp = active.get(0);
        if (otp.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            return false;
        }
        if (encoder.matches(submitted, otp.getOtpHash())) {
            otp.markConsumed(now);
            return true;
        }
        otp.recordFailure();
        return false;
    }
}