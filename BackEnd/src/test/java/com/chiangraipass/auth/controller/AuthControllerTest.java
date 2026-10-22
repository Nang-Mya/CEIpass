package com.chiangraipass.auth.controller;

import java.util.Optional;
import java.util.UUID;

import com.chiangraipass.auth.model.EmailVisitorPrincipal;
import com.chiangraipass.auth.model.VisitorAccount;
import com.chiangraipass.auth.service.VisitorAccountService;
import com.chiangraipass.auth.service.VisitorOAuth2UserService;
import com.chiangraipass.auth.service.EmailVerificationService;
import com.chiangraipass.config.CorsConfiguration;
import com.chiangraipass.config.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = AuthController.class,
        properties = {
                "app.privacy.notice-version=test-notice-v1",
                "app.privacy.notice-url=https://example.test/privacy",
                "app.auth.registration-enabled=true"
        })
@Import({CorsConfiguration.class, SecurityConfiguration.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private VisitorAccountService accountService;

    @MockitoBean
    private EmailVerificationService emailVerification;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private VisitorOAuth2UserService oauthUserService;

    @Test
    void exposesRegistrationAvailabilityAndDoesNotCacheAccountConfiguration() throws Exception {
        mockMvc.perform(get("/api/v1/auth/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationEnabled").value(true))
                .andExpect(jsonPath("$.privacyNoticeUrl").value("https://example.test/privacy"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("oauthProviders")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    }

    @Test
    void createsAccountWithHashedPasswordAndAcknowledgedNotice() throws Exception {
        given(accountService.findByEmail("visitor@example.com")).willReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "fullName": "Chiang Rai Visitor",
                                  "email": "Visitor@Example.com",
                                  "password": "Correct-horse-battery-1",
                                  "privacyNoticeAcknowledged": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Account created. Check your email and open the verification link before signing in."));

        ArgumentCaptor<String> passwordHash = ArgumentCaptor.forClass(String.class);
        verify(accountService).createEmailAccount(
                any(UUID.class),
                eq("visitor@example.com"),
                eq("Chiang Rai Visitor"),
                passwordHash.capture(),
                eq("test-notice-v1"));
        org.junit.jupiter.api.Assertions.assertTrue(passwordHash.getValue().startsWith("$2a$12$"));
        org.junit.jupiter.api.Assertions.assertNotEquals("Correct-horse-battery-1", passwordHash.getValue());
        verify(emailVerification).sendVerification(
                any(UUID.class), eq("visitor@example.com"), eq("Chiang Rai Visitor"));
    }

    @Test
    void rejectsRegistrationWithoutPrivacyNoticeAcknowledgement() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "fullName": "Chiang Rai Visitor",
                                  "email": "visitor@example.com",
                                  "password": "Correct-horse-battery-1",
                                  "privacyNoticeAcknowledged": false
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsUnauthorizedForAccountDetailsWithoutSession() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Sign in is required."));
    }

    @Test
    void signsInWithEmailAndPasswordAndKeepsSessionAuthenticated() throws Exception {
        String email = "visitor@example.com";
        String password = "Correct-horse-battery-1";
        VisitorAccount account = new VisitorAccount(
                UUID.randomUUID(), email, "Chiang Rai Visitor", passwordEncoder.encode(password));
        given(userDetailsService.loadUserByUsername(email)).willReturn(new EmailVisitorPrincipal(account));

        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(APPLICATION_FORM_URLENCODED)
                        .param("email", email)
                        .param("password", password))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.displayName").value("Chiang Rai Visitor"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.accountId").value(account.accountId().toString()));
    }

    @Test
    void requiresCsrfForAccountCreation() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content("""
                                {
                                  "fullName": "Chiang Rai Visitor",
                                  "email": "visitor@example.com",
                                  "password": "Correct-horse-battery-1",
                                  "privacyNoticeAcknowledged": true
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void csrfCookieTokenIsAcceptedInTheSpaRequestHeader() throws Exception {
        given(accountService.findByEmail("visitor@example.com")).willReturn(Optional.empty());
        MvcResult tokenResult = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andReturn();
        jakarta.servlet.http.Cookie tokenCookie = tokenResult.getResponse().getCookie("XSRF-TOKEN");
        org.junit.jupiter.api.Assertions.assertNotNull(tokenCookie);

        mockMvc.perform(post("/api/v1/auth/register")
                        .cookie(new jakarta.servlet.http.Cookie("XSRF-TOKEN", tokenCookie.getValue()))
                        .header("X-XSRF-TOKEN", tokenCookie.getValue())
                        .contentType("application/json")
                        .content("""
                                {
                                  "fullName": "Chiang Rai Visitor",
                                  "email": "visitor@example.com",
                                  "password": "Correct-horse-battery-1",
                                  "privacyNoticeAcknowledged": true
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void explainsWhyAWeakPasswordWasRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "fullName": "Chiang Rai Visitor",
                                  "email": "visitor@example.com",
                                  "password": "abc",
                                  "privacyNoticeAcknowledged": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("at least 8 characters"),
                        org.hamcrest.Matchers.containsString("at least 1 number"),
                        org.hamcrest.Matchers.containsString("special character"))));
    }

    @Test
    void explainsWhyANonEnglishPasswordWasRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"fullName\":\"Visitor\",\"email\":\"visitor@example.com\","
                                + "\"password\":\"\u0e23\u0e2b\u0e31\u0e2a\u0e1c\u0e48\u0e32\u0e191!ab\","
                                + "\"privacyNoticeAcknowledged\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("only English letters")));
    }

    @Test
    void explainsAnInvalidEmailAddress() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "fullName": "Chiang Rai Visitor",
                                  "email": "not-an-email",
                                  "password": "Correct-horse-battery-1",
                                  "privacyNoticeAcknowledged": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Enter a valid email address."));
    }

    @Test
    void explainsMissingPrivacyAcknowledgement() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "fullName": "Chiang Rai Visitor",
                                  "email": "visitor@example.com",
                                  "password": "Correct-horse-battery-1",
                                  "privacyNoticeAcknowledged": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Acknowledge the published privacy notice before creating an account."));
    }

    @Test
    void wrongPasswordGetsTheGenericMessage() throws Exception {
        VisitorAccount account = new VisitorAccount(UUID.randomUUID(), "visitor@example.com",
                "Chiang Rai Visitor", passwordEncoder.encode("Correct-horse-battery-1"));
        given(userDetailsService.loadUserByUsername("visitor@example.com"))
                .willReturn(new EmailVisitorPrincipal(account));

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(APPLICATION_FORM_URLENCODED)
                        .param("email", "visitor@example.com")
                        .param("password", "Wrong-password-1!"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    @Test
    void lockedAccountGetsTheLockMessageEvenWithTheCorrectPassword() throws Exception {
        VisitorAccount locked = new VisitorAccount(UUID.randomUUID(), "visitor@example.com",
                "Chiang Rai Visitor", passwordEncoder.encode("Correct-horse-battery-1"),
                true, java.time.Instant.now().plusSeconds(300));
        given(userDetailsService.loadUserByUsername("visitor@example.com"))
                .willReturn(new EmailVisitorPrincipal(locked));

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(APPLICATION_FORM_URLENCODED)
                        .param("email", "visitor@example.com")
                        .param("password", "Correct-horse-battery-1"))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("locked for 10 minutes")));
    }

    @Test
    void verifiesTheEmailWithAValidToken() throws Exception {
        given(emailVerification.verify("good-token")).willReturn(true);

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"token\":\"good-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified. You can now sign in."));
    }

    @Test
    void rejectsAnInvalidOrExpiredVerificationToken() throws Exception {
        given(emailVerification.verify("old-token")).willReturn(false);

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"token\":\"old-token\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("invalid or has expired")));
    }

    @Test
    void requiresATokenToVerifyTheEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"token\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Verification token is required."));
    }

    @Test
    void resendGivesTheSameAnswerWhetherOrNotTheEmailHasAnAccount() throws Exception {
        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"email\":\"nobody@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("If that email belongs to an account")));

        verify(emailVerification).resend("nobody@example.com");
    }

    @Test
    void unverifiedAccountWithTheCorrectPasswordIsToldToVerifyItsEmail() throws Exception {
        VisitorAccount account = new VisitorAccount(UUID.randomUUID(), "visitor@example.com",
                "Chiang Rai Visitor", passwordEncoder.encode("Correct-horse-battery-1"), false, null);
        given(userDetailsService.loadUserByUsername("visitor@example.com"))
                .willReturn(new EmailVisitorPrincipal(account));

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(APPLICATION_FORM_URLENCODED)
                        .param("email", "visitor@example.com")
                        .param("password", "Correct-horse-battery-1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("verify your email")));
    }

    @Test
    void unverifiedAccountWithAWrongPasswordGetsTheGenericMessage() throws Exception {
        VisitorAccount account = new VisitorAccount(UUID.randomUUID(), "visitor@example.com",
                "Chiang Rai Visitor", passwordEncoder.encode("Correct-horse-battery-1"), false, null);
        given(userDetailsService.loadUserByUsername("visitor@example.com"))
                .willReturn(new EmailVisitorPrincipal(account));

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(APPLICATION_FORM_URLENCODED)
                        .param("email", "visitor@example.com")
                        .param("password", "Wrong-password-1!"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }
}
