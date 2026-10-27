package com.chiangraipass.auth.controller;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.chiangraipass.auth.model.EmailVisitorPrincipal;
import com.chiangraipass.auth.model.VisitorAccount;
import com.chiangraipass.auth.service.LoginAttemptService;
import com.chiangraipass.auth.service.VisitorAccountService;
import com.chiangraipass.auth.service.VisitorOAuth2UserService;
import com.chiangraipass.config.CorsConfiguration;
import com.chiangraipass.config.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfileController.class)
@Import({CorsConfiguration.class, SecurityConfiguration.class})
class ProfileControllerTest {

    private static final String EMAIL = "visitor@example.com";
    private static final String CURRENT = "Old-password-1!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private VisitorAccountService accountService;

    @MockitoBean
    private LoginAttemptService loginAttempts;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private VisitorOAuth2UserService oauthUserService;

    private final UUID id = UUID.randomUUID();

    private RequestPostProcessor signedIn() {
        EmailVisitorPrincipal principal =
                new EmailVisitorPrincipal(new VisitorAccount(id, EMAIL, "Old Name", "hash"));
        return authentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()));
    }

    private VisitorAccount account(String passwordHash, Instant lockedUntil) {
        return new VisitorAccount(id, EMAIL, "Old Name", passwordHash, true, lockedUntil);
    }

    private void accountExists(VisitorAccount account) {
        given(accountService.findById(id)).willReturn(Optional.of(account));
    }

    private org.springframework.test.web.servlet.ResultActions changePassword(
            String current, String next) throws Exception {
        return mockMvc.perform(post("/api/v1/me/password")
                .with(signedIn()).with(csrf())
                .contentType("application/json")
                .content("{\"currentPassword\":\"" + current + "\",\"newPassword\":\"" + next + "\"}"));
    }

    @Test
    void updatesTheDisplayNameAfterTrimmingIt() throws Exception {
        given(accountService.updateDisplayName(id, "New Name")).willReturn(true);
        accountExists(new VisitorAccount(id, EMAIL, "New Name", "hash"));

        mockMvc.perform(patch("/api/v1/me/profile")
                        .with(signedIn()).with(csrf())
                        .contentType("application/json")
                        .content("{\"displayName\":\"  New Name  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("New Name"))
                .andExpect(jsonPath("$.hasPassword").value(true));

        verify(accountService).updateDisplayName(id, "New Name");
    }

    @Test
    void rejectsABlankName() throws Exception {
        mockMvc.perform(patch("/api/v1/me/profile")
                        .with(signedIn()).with(csrf())
                        .contentType("application/json")
                        .content("{\"displayName\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Name is required."));
    }

    @Test
    void rejectsControlCharactersInTheName() throws Exception {
        mockMvc.perform(patch("/api/v1/me/profile")
                        .with(signedIn()).with(csrf())
                        .contentType("application/json")
                        .content("{\"displayName\":\"Bad\\u0007Name\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Name contains characters that are not allowed."));

        verify(accountService, never()).updateDisplayName(any(), any());
    }

    @Test
    void requiresSignInToEditTheProfile() throws Exception {
        mockMvc.perform(patch("/api/v1/me/profile")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"displayName\":\"New Name\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changesThePasswordWhenTheCurrentOneIsCorrect() throws Exception {
        accountExists(account(passwordEncoder.encode(CURRENT), null));

        changePassword(CURRENT, "Brand-new-pass-1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("Password updated")));

        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(accountService).changePassword(eq(id), hash.capture());
        org.junit.jupiter.api.Assertions.assertTrue(hash.getValue().startsWith("$2a$12$"));
    }

    @Test
    void aWrongCurrentPasswordIsRefusedAndCountedAsAFailedAttempt() throws Exception {
        accountExists(account(passwordEncoder.encode(CURRENT), null));

        changePassword("Not-the-password-1!", "Brand-new-pass-1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Your current password is incorrect."));

        verify(loginAttempts).recordFailure(EMAIL);
        verify(accountService, never()).changePassword(any(), any());
    }

    @Test
    void aLockedAccountCannotChangeItsPassword() throws Exception {
        accountExists(account(passwordEncoder.encode(CURRENT), Instant.now().plusSeconds(300)));

        changePassword(CURRENT, "Brand-new-pass-1")
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("locked")));

        verify(loginAttempts, never()).recordFailure(any());
        verify(accountService, never()).changePassword(any(), any());
    }

    @Test
    void aWeakNewPasswordIsExplained() throws Exception {
        accountExists(account(passwordEncoder.encode(CURRENT), null));

        changePassword(CURRENT, "abc")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("at least 8 characters")));

        verify(accountService, never()).changePassword(any(), any());
    }

    @Test
    void theNewPasswordMustDifferFromTheCurrentOne() throws Exception {
        accountExists(account(passwordEncoder.encode(CURRENT), null));

        changePassword(CURRENT, CURRENT)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("different")));
    }

    @Test
    void anAccountWithoutAPasswordHasNothingToChange() throws Exception {
        accountExists(account(null, null));

        changePassword(CURRENT, "Brand-new-pass-1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("no password")));
    }
}