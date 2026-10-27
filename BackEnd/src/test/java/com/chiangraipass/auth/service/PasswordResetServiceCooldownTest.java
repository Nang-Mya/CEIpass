package com.chiangraipass.auth.service;

import java.util.Optional;
import java.util.UUID;

import com.chiangraipass.auth.model.VisitorAccount;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class PasswordResetServiceCooldownTest {

    private final PasswordResetOtpService otps = mock(PasswordResetOtpService.class);
    private final AccountTokenService tokens = mock(AccountTokenService.class);
    private final VisitorAccountService accounts = mock(VisitorAccountService.class);
    private final MailService mail = mock(MailService.class);
    private final PasswordResetService service = new PasswordResetService(otps, tokens, accounts, mail);

    @Test
    void aSecondRequestForTheSameEmailSendsNothingAndSaysHowLongToWait() {
        VisitorAccount account = new VisitorAccount(UUID.randomUUID(), "visitor@example.com", "Visitor", "hash");
        given(accounts.findByEmail("visitor@example.com")).willReturn(Optional.of(account));
        given(otps.issue(account.accountId())).willReturn("123456");

        assertThat(service.requestOtp("visitor@example.com")).isZero();
        long wait = service.requestOtp("visitor@example.com");

        assertThat(wait).isBetween(1L, 60L);
        verify(mail, times(1)).send(eq("visitor@example.com"), any(), any());
        verify(otps, times(1)).issue(account.accountId());
    }

    @Test
    void anUnknownEmailGetsTheSameWaitAndNothingIsSent() {
        given(accounts.findByEmail("nobody@example.com")).willReturn(Optional.empty());

        assertThat(service.requestOtp("nobody@example.com")).isZero();
        assertThat(service.requestOtp("nobody@example.com")).isBetween(1L, 60L);

        verify(mail, never()).send(any(), any(), any());
    }
}