package com.chiangraipass.auth.service;

import java.util.Optional;
import java.util.UUID;

import com.chiangraipass.auth.model.AccountTokenPurpose;
import com.chiangraipass.auth.model.VisitorAccount;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class EmailVerificationServiceTest {

    private final AccountTokenService tokens = mock(AccountTokenService.class);
    private final VisitorAccountService accounts = mock(VisitorAccountService.class);
    private final MailService mail = mock(MailService.class);
    private final EmailVerificationService service =
            new EmailVerificationService(tokens, accounts, mail, "http://localhost:5500/");
    private final UUID id = UUID.randomUUID();

    private VisitorAccount account(String passwordHash, boolean verified) {
        return new VisitorAccount(id, "a@example.com", "Alice", passwordHash, verified, null);
    }

    @Test
    void emailsALinkContainingTheTokenInTheUrlFragment() {
        given(tokens.issue(id, AccountTokenPurpose.VERIFY_EMAIL, EmailVerificationService.TOKEN_LIFETIME))
                .willReturn("tok123");

        boolean sent = service.sendVerification(id, "a@example.com", "Alice");

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(mail).send(eq("a@example.com"), anyString(), body.capture());
        assertThat(sent).isTrue();
        assertThat(body.getValue())
                .contains("http://localhost:5500/#verify?token=tok123")
                .contains("Alice");
    }

    @Test
    void reportsFailureInsteadOfThrowingWhenTheEmailCannotBeSent() {
        given(tokens.issue(any(), any(), any())).willReturn("tok");
        org.mockito.Mockito.doThrow(new IllegalStateException("smtp down"))
                .when(mail).send(any(), any(), any());

        assertThat(service.sendVerification(id, "a@example.com", "Alice")).isFalse();
    }

    @Test
    void validTokenVerifiesTheAccount() {
        given(tokens.consume("good", AccountTokenPurpose.VERIFY_EMAIL)).willReturn(Optional.of(id));
        given(accounts.markEmailVerified(id)).willReturn(true);

        assertThat(service.verify("good")).isTrue();
    }

    @Test
    void unknownTokenVerifiesNothing() {
        given(tokens.consume("bad", AccountTokenPurpose.VERIFY_EMAIL)).willReturn(Optional.empty());

        assertThat(service.verify("bad")).isFalse();
        verify(accounts, never()).markEmailVerified(any());
    }

    @Test
    void resendSendsALinkToAnUnverifiedPasswordAccount() {
        given(accounts.findByEmail("a@example.com")).willReturn(Optional.of(account("hash", false)));
        given(tokens.issue(any(), any(), any())).willReturn("tok");

        service.resend("a@example.com");

        verify(mail).send(eq("a@example.com"), anyString(), anyString());
    }

    @Test
    void resendDoesNothingForVerifiedUnknownOrPasswordlessAccounts() {
        given(accounts.findByEmail("verified@example.com")).willReturn(Optional.of(account("hash", true)));
        given(accounts.findByEmail("oauth@example.com")).willReturn(Optional.of(account(null, false)));
        given(accounts.findByEmail("nobody@example.com")).willReturn(Optional.empty());

        service.resend("verified@example.com");
        service.resend("oauth@example.com");
        service.resend("nobody@example.com");

        verify(mail, never()).send(any(), any(), any());
    }

    @Test
    void resendIsSkippedDuringTheCooldown() {
        given(accounts.findByEmail("a@example.com")).willReturn(Optional.of(account("hash", false)));
        given(tokens.issuedRecently(id, AccountTokenPurpose.VERIFY_EMAIL,
                EmailVerificationService.RESEND_COOLDOWN)).willReturn(true);

        service.resend("a@example.com");

        verify(mail, never()).send(any(), any(), any());
    }
}