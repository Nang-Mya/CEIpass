package com.chiangraipass.auth.controller;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import com.chiangraipass.auth.model.EmailVisitorPrincipal;
import com.chiangraipass.auth.model.VisitorAccount;
import com.chiangraipass.auth.service.PasswordResetService;
import com.chiangraipass.auth.service.VisitorAccountService;
import com.chiangraipass.auth.service.VisitorOAuth2UserService;
import com.chiangraipass.config.CorsConfiguration;
import com.chiangraipass.config.SecurityConfiguration;

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
    private UserDetailsService userDetailsService;

    @MockitoBean
    private VisitorOAuth2UserService oauthUserService;

        @MockitoBean
    private PasswordResetService passwordReset;



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
                .andExpect(jsonPath("$.message").value("Account created. Sign in with your email and password."));

        ArgumentCaptor<String> passwordHash = ArgumentCaptor.forClass(String.class);
        verify(accountService).createEmailAccount(
                any(UUID.class),
                eq("visitor@example.com"),
                eq("Chiang Rai Visitor"),
                passwordHash.capture(),
                eq("test-notice-v1"));
        org.junit.jupiter.api.Assertions.assertTrue(passwordHash.getValue().startsWith("$2a$12$"));
        org.junit.jupiter.api.Assertions.assertNotEquals("Correct-horse-battery-1", passwordHash.getValue());
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
                UUID.randomUUID(), email, "Chiang Rai Visitor", passwordEncoder.encode(password),null);
        given(userDetailsService.loadUserByUsername(email)).willReturn(new EmailVisitorPrincipal(account));
        given(accountService.findById(account.accountId())).willReturn(Optional.of(account));
        given(accountService.findById(account.accountId())).willReturn(Optional.of(account));
        
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
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
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
                "Chiang Rai Visitor", passwordEncoder.encode("Correct-horse-battery-1"),null);
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
                true, java.time.Instant.now().plusSeconds(300),null);
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
    void forgotPasswordGivesTheSameAnswerForAnyEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/password/forgot")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"email\":\"nobody@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("If that email belongs to an account")));

        verify(passwordReset).requestOtp("nobody@example.com");
    }

    @Test
    void aCorrectCodeReturnsAResetToken() throws Exception {
        given(passwordReset.verifyOtp("visitor@example.com", "123456"))
                .willReturn(java.util.Optional.of("grant-token"));

        mockMvc.perform(post("/api/v1/auth/password/verify-otp")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"email\":\"visitor@example.com\",\"otp\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("grant-token"));
    }

    @Test
    void aWrongOrExpiredCodeIsExplained() throws Exception {
        given(passwordReset.verifyOtp("visitor@example.com", "000000"))
                .willReturn(java.util.Optional.empty());

        mockMvc.perform(post("/api/v1/auth/password/verify-otp")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"email\":\"visitor@example.com\",\"otp\":\"000000\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("incorrect or has expired")));
    }

    @Test
    void aCodeThatIsNotSixDigitsIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/password/verify-otp")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"email\":\"visitor@example.com\",\"otp\":\"12ab\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Enter the 6-digit code."));

        verify(passwordReset, org.mockito.Mockito.never()).verifyOtp(any(), any());
    }

    @Test
    void resetsThePasswordWithAValidTokenAndAStrongPassword() throws Exception {
        given(passwordReset.reset(eq("grant-token"), any(String.class))).willReturn(true);

        mockMvc.perform(post("/api/v1/auth/password/reset")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"token\":\"grant-token\",\"newPassword\":\"Brand-new-pass-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password updated. Sign in with your new password."));
    }

    @Test
    void aWeakNewPasswordIsRejectedWithoutUsingUpTheToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/password/reset")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"token\":\"grant-token\",\"newPassword\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("at least 8 characters")));

        verify(passwordReset, org.mockito.Mockito.never()).reset(any(), any());
    }

    @Test
    void anExpiredResetTokenIsRejected() throws Exception {
        given(passwordReset.reset(eq("old-token"), any(String.class))).willReturn(false);

        mockMvc.perform(post("/api/v1/auth/password/reset")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"token\":\"old-token\",\"newPassword\":\"Brand-new-pass-1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("has expired")));
    }

    @Test
    void forgotPasswordTellsHowLongToWaitWhileTheCooldownRuns() throws Exception {
        given(passwordReset.requestOtp("visitor@example.com")).willReturn(42L);

        mockMvc.perform(post("/api/v1/auth/password/forgot")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"email\":\"visitor@example.com\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "42"))
                .andExpect(jsonPath("$.retryAfterSeconds").value(42))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("42 seconds")));
    }

    @Test
    void forgotPasswordReportsTheCooldownThatStartsNow() throws Exception {
        mockMvc.perform(post("/api/v1/auth/password/forgot")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"email\":\"visitor@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retryAfterSeconds").value(60));
    }

    @Test
    void meRejectsASessionWhoseAccountNoLongerExists() throws Exception {
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
                .andReturn();

        given(accountService.findById(account.accountId())).willReturn(Optional.empty());

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }
//     @Test
//     void csrfCookieTokenIsAcceptedInTheSpaRequestHeader() throws Exception {
//         given(accountService.findByEmail("visitor@example.com")).willReturn(Optional.empty());
//         MvcResult tokenResult = mockMvc.perform(get("/api/v1/auth/csrf"))
//                 .andExpect(status().isOk())
//                 .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
//                         .header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
//                 .andReturn();
//         jakarta.servlet.http.Cookie tokenCookie = tokenResult.getResponse().getCookie("XSRF-TOKEN");
//         org.junit.jupiter.api.Assertions.assertNotNull(tokenCookie);

//         mockMvc.perform(post("/api/v1/auth/register")
//                         .cookie(new MockCookie("XSRF-TOKEN", tokenCookie.getValue()))
//                         .header("X-XSRF-TOKEN", tokenCookie.getValue())
//                         .contentType("application/json")
//                         .content("""
//                                 {
//                                   "fullName": "Chiang Rai Visitor",
//                                   "email": "visitor@example.com",
//                                   "password": "correct horse battery staple",
//                                   "privacyNoticeAcknowledged": true
//                                 }
//                                 """))
//                 .andExpect(status().isCreated());
//     }
}
