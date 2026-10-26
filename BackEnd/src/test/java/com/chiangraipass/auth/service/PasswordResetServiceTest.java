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

class PasswordResetServiceTest {

    private final PasswordResetOtpService otps = mock(PasswordResetOtpService.class);
    private final AccountTokenService tokens = mock(AccountTokenService.class);
    private final VisitorAccountService accounts = mock(VisitorAccountService.class);
    private final MailService mail = mock(MailService.class);
    private final PasswordResetService service = new PasswordResetService(otps, tokens, accounts, mail);
    private final UUID id = UUID.randomUUID();

    private VisitorAccount account(String passwordHash) {
        return new VisitorAccount(id, "a@example.com", "Alice", passwordHash,null);
    }

    @Test
    void emailsTheCodeToAnEmailPasswordAccount() {
        given(accounts.findByEmail("a@example.com")).willReturn(Optional.of(account("hash")));
        given(otps.issue(id)).willReturn("123456");

        service.requestOtp("a@example.com");

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(mail).send(eq("a@example.com"), anyString(), body.capture());
        assertThat(body.getValue()).contains("123456").contains("10 minutes");
    }

    @Test
    void sendsNothingForUnknownEmailsOrAccountsWithoutAPassword() {
        given(accounts.findByEmail("nobody@example.com")).willReturn(Optional.empty());
        given(accounts.findByEmail("oauth@example.com")).willReturn(Optional.of(account(null)));

        service.requestOtp("nobody@example.com");
        service.requestOtp("oauth@example.com");

        verify(otps, never()).issue(any());
        verify(mail, never()).send(any(), any(), any());
    }

    @Test
    void sendsNothingDuringTheCooldown() {
        given(accounts.findByEmail("a@example.com")).willReturn(Optional.of(account("hash")));
        given(otps.issuedRecently(id, PasswordResetService.REQUEST_COOLDOWN)).willReturn(true);

        service.requestOtp("a@example.com");

        verify(otps, never()).issue(any());
        verify(mail, never()).send(any(), any(), any());
    }

    @Test
    void aMailFailureDoesNotEscapeToTheCaller() {
        given(accounts.findByEmail("a@example.com")).willReturn(Optional.of(account("hash")));
        given(otps.issue(id)).willReturn("123456");
        org.mockito.Mockito.doThrow(new IllegalStateException("smtp down"))
                .when(mail).send(any(), any(), any());

        service.requestOtp("a@example.com");

        verify(mail).send(any(), any(), any());
    }

    @Test
    void aCorrectCodeGivesAResetToken() {
        given(accounts.findByEmail("a@example.com")).willReturn(Optional.of(account("hash")));
        given(otps.verify(id, "123456")).willReturn(true);
        given(tokens.issue(id, AccountTokenPurpose.PASSWORD_RESET, PasswordResetService.GRANT_LIFETIME))
                .willReturn("grant");

        assertThat(service.verifyOtp("a@example.com", "123456")).contains("grant");
    }

    @Test
    void aWrongCodeGivesNoToken() {
        given(accounts.findByEmail("a@example.com")).willReturn(Optional.of(account("hash")));
        given(otps.verify(id, "000000")).willReturn(false);

        assertThat(service.verifyOtp("a@example.com", "000000")).isEmpty();
        verify(tokens, never()).issue(any(), any(), any());
    }

    @Test
    void anUnknownEmailGivesNoTokenAndChecksNoCode() {
        given(accounts.findByEmail("nobody@example.com")).willReturn(Optional.empty());

        assertThat(service.verifyOtp("nobody@example.com", "123456")).isEmpty();
        verify(otps, never()).verify(any(), any());
    }

    @Test
    void aValidTokenChangesThePassword() {
        given(tokens.consume("grant", AccountTokenPurpose.PASSWORD_RESET)).willReturn(Optional.of(id));
        given(accounts.changePassword(id, "new-hash")).willReturn(true);

        assertThat(service.reset("grant", "new-hash")).isTrue();
    }

    @Test
    void anUnknownTokenChangesNothing() {
        given(tokens.consume("bad", AccountTokenPurpose.PASSWORD_RESET)).willReturn(Optional.empty());

        assertThat(service.reset("bad", "new-hash")).isFalse();
        verify(accounts, never()).changePassword(any(), any());
    }
}