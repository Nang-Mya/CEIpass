package com.chiangraipass.auth.service;

import com.chiangraipass.auth.model.EmailVisitorPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/** Feeds Spring Security sign-in results into {@link LoginAttemptService}. */
@Component
public class LoginAttemptListener {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptListener.class);

    private final LoginAttemptService attempts;

    public LoginAttemptListener(LoginAttemptService attempts) {
        this.attempts = attempts;
    }

    @EventListener
    public void onBadCredentials(AuthenticationFailureBadCredentialsEvent event) {
        try {
            attempts.recordFailure(event.getAuthentication().getName());
        } catch (RuntimeException error) {
            // Never turn a counting problem into a different sign-in response.
            log.warn("Could not record failed sign-in attempt.", error);
        }
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        if (event.getAuthentication().getPrincipal() instanceof EmailVisitorPrincipal principal) {
            try {
                attempts.recordSuccess(principal.accountId());
            } catch (RuntimeException error) {
                log.warn("Could not clear failed sign-in attempts.", error);
            }
        }
    }
}