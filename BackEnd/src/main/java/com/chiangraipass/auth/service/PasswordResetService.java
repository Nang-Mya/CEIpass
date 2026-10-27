package com.chiangraipass.auth.service;

import java.time.Duration;
import java.util.Optional;

import com.chiangraipass.auth.model.AccountTokenPurpose;
import com.chiangraipass.auth.model.VisitorAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Password reset by emailed code: request a code, verify it, then set a new password. */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    static final Duration REQUEST_COOLDOWN = Duration.ofMinutes(1);
    static final Duration GRANT_LIFETIME = Duration.ofMinutes(10);

    private final PasswordResetOtpService otps;
    private final AccountTokenService tokens;
    private final VisitorAccountService accounts;
    private final MailService mail;
    private final OtpRequestCooldown cooldown;

    @Autowired
    public PasswordResetService(
            PasswordResetOtpService otps,
            AccountTokenService tokens,
            VisitorAccountService accounts,
            MailService mail) {
        this(otps, tokens, accounts, mail, new OtpRequestCooldown());
    }

    PasswordResetService(
            PasswordResetOtpService otps,
            AccountTokenService tokens,
            VisitorAccountService accounts,
            MailService mail,
            OtpRequestCooldown cooldown) {
        this.otps = otps;
        this.tokens = tokens;
        this.accounts = accounts;
        this.mail = mail;
        this.cooldown = cooldown;
    }

    /**
     * Emails a 6-digit code to an existing email/password account and returns 0. If a code was
     * requested for this email less than a minute ago nothing is sent and the whole seconds left
     * to wait are returned. The cooldown applies to every email address alike, so the answer does
     * not reveal which emails have accounts.
     */
    public long requestOtp(String email) {
        long secondsToWait = cooldown.tryStart(email);
        if (secondsToWait > 0) {
            return secondsToWait;
        }
        accounts.findByEmail(email)
                .filter(account -> account.passwordHash() != null)
                .filter(account -> !otps.issuedRecently(account.accountId(), REQUEST_COOLDOWN))
                .ifPresent(this::sendCode);
        return 0;
    }

    /** Returns a short-lived single-use token for choosing a new password if the code is right. */
    public Optional<String> verifyOtp(String email, String code) {
        return accounts.findByEmail(email)
                .filter(account -> account.passwordHash() != null)
                .filter(account -> otps.verify(account.accountId(), code))
                .map(account -> tokens.issue(
                        account.accountId(), AccountTokenPurpose.PASSWORD_RESET, GRANT_LIFETIME));
    }

    /** Applies the new password hash if the token from {@link #verifyOtp} is valid and unused. */
    public boolean reset(String token, String newPasswordHash) {
        return tokens.consume(token, AccountTokenPurpose.PASSWORD_RESET)
                .map(accountId -> accounts.changePassword(accountId, newPasswordHash))
                .orElse(false);
    }

    private void sendCode(VisitorAccount account) {
        try {
            String code = otps.issue(account.accountId());
            mail.send(account.email(), "Your Chiang Rai Pass password reset code",
                    body(account.displayName(), code));
        } catch (RuntimeException error) {
            log.warn("Could not send a password reset code.", error);
        }
    }

    private String body(String displayName, String code) {
        return "Hello " + displayName + ",\n\n"
                + "Your Chiang Rai Pass password reset code is:\n\n"
                + "    " + code + "\n\n"
                + "It expires in 10 minutes and can be used once. Do not share it with anyone. "
                + "If you did not ask to reset your password, ignore this email; "
                + "your password stays the same.\n";
    }
}