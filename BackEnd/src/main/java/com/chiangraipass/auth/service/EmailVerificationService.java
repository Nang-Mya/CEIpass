package com.chiangraipass.auth.service;

import java.time.Duration;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.chiangraipass.auth.model.AccountTokenPurpose;

/** Sends verification links and turns a valid link into a verified email address. */
@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    static final Duration TOKEN_LIFETIME = Duration.ofHours(24);
    static final Duration RESEND_COOLDOWN = Duration.ofMinutes(1);

    private final AccountTokenService tokens;
    private final VisitorAccountService accounts;
    private final MailService mail;
    private final String frontendUrl;

    public EmailVerificationService(
            AccountTokenService tokens,
            VisitorAccountService accounts,
            MailService mail,
            @Value("${app.auth.frontend-url:http://localhost:8080}") String frontendUrl) {
        this.tokens = tokens;
        this.accounts = accounts;
        this.mail = mail;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    /** Emails a new link. Returns false (without throwing) if the email could not be sent. */
    public boolean sendVerification(UUID accountId, String email, String displayName) {
        try {
            String token = tokens.issue(accountId, AccountTokenPurpose.VERIFY_EMAIL, TOKEN_LIFETIME);
            String link = frontendUrl + "/#verify?token=" + token;
            mail.send(email, "Verify your Chiang Rai Pass email", body(displayName, link));
            return true;
        } catch (RuntimeException error) {
            log.warn("Could not send a verification email.", error);
            return false;
        }
    }

    /** True if the token was valid, unused and unexpired, and the account is now verified. */
    public boolean verify(String token) {
        return tokens.consume(token, AccountTokenPurpose.VERIFY_EMAIL)
                .map(accounts::markEmailVerified)
                .orElse(false);
    }

    /** Sends a new link only for an unverified email/password account; otherwise does nothing. */
    public void resend(String email) {
        accounts.findByEmail(email)
                .filter(account -> account.passwordHash() != null && !account.emailVerified())
                .filter(account -> !tokens.issuedRecently(
                        account.accountId(), AccountTokenPurpose.VERIFY_EMAIL, RESEND_COOLDOWN))
                .ifPresent(account ->
                        sendVerification(account.accountId(), account.email(), account.displayName()));
    }

    private String body(String displayName, String link) {
        return "Hello " + displayName + ",\n\n"
                + "Confirm your email address to finish creating your Chiang Rai Pass account:\n\n"
                + link + "\n\n"
                + "This link can be used once and expires in 24 hours. "
                + "If you did not create an account, you can ignore this email.\n";
    }
}