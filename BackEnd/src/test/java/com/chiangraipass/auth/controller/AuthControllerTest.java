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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chiangraipass.auth.model.EmailVisitorPrincipal;
import com.chiangraipass.auth.model.VisitorAccount;
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
                                  "password": "correct horse battery staple",
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
        org.junit.jupiter.api.Assertions.assertNotEquals("correct horse battery staple", passwordHash.getValue());
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
                                  "password": "correct horse battery staple",
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
        String password = "correct horse battery staple";
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

//     @Test
//     void requiresCsrfForAccountCreation() throws Exception {
//         mockMvc.perform(post("/api/v1/auth/register")
//                         .contentType("application/json")
//                         .content("""
//                                 {
//                                   "fullName": "Chiang Rai Visitor",
//                                   "email": "visitor@example.com",
//                                   "password": "correct horse battery staple",
//                                   "privacyNoticeAcknowledged": true
//                                 }
//                                 """))
//                 .andExpect(status().isForbidden());
//     }

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
