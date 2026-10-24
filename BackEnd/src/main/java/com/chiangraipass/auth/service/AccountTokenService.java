package com.chiangraipass.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import com.chiangraipass.auth.entity.AccountTokenEntity;
import com.chiangraipass.auth.model.AccountTokenPurpose;
import com.chiangraipass.auth.repository.AccountTokenJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues and redeems single-use account tokens. The raw token is returned once (to be emailed)
 * and never stored; only its SHA-256 hash is persisted.
 */
@Service
public class AccountTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final AccountTokenJpaRepository tokens;
    private final Clock clock;

    @Autowired
    public AccountTokenService(AccountTokenJpaRepository tokens) {
        this(tokens, Clock.systemUTC());
    }

    AccountTokenService(AccountTokenJpaRepository tokens, Clock clock) {
        this.tokens = tokens;
        this.clock = clock;
    }

    /** Creates a new token, invalidating any earlier unused token of the same purpose. */
    @Transactional
    public String issue(UUID accountId, AccountTokenPurpose purpose, Duration lifetime) {
        if (lifetime.isZero() || lifetime.isNegative()) {
            throw new IllegalArgumentException("Token lifetime must be positive.");
        }
        Instant now = clock.instant();
        tokens.invalidateOutstanding(accountId, purpose, now);

        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        tokens.save(new AccountTokenEntity(accountId, purpose, hash(rawToken), now, now.plus(lifetime)));
        return rawToken;
    }

    /**
     * Redeems a token. Returns the owning account id if the token is valid for this purpose,
     * unexpired and unused; otherwise empty. Succeeds at most once per token.
     */
    @Transactional
    public Optional<UUID> consume(String rawToken, AccountTokenPurpose purpose) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > 128) {
            return Optional.empty();
        }
        String hash = hash(rawToken.strip());
        if (tokens.consume(hash, purpose, clock.instant()) != 1) {
            return Optional.empty();
        }
        return tokens.findByTokenHash(hash).map(AccountTokenEntity::getAccountId);
    }

    /** Housekeeping: removes tokens that expired before the retention cutoff. */
    @Transactional
    public int purgeExpired(Duration retention) {
        return tokens.deleteExpiredBefore(clock.instant().minus(retention));
    }

    static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable.", e);
        }
    }
}