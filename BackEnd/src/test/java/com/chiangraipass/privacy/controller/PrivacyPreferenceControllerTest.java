package com.chiangraipass.privacy.controller;

import java.util.List;
import java.util.UUID;

import com.chiangraipass.auth.model.EmailVisitorPrincipal;
import com.chiangraipass.auth.model.VisitorAccount;
import com.chiangraipass.auth.service.VisitorOAuth2UserService;
import com.chiangraipass.config.CorsConfiguration;
import com.chiangraipass.config.SecurityConfiguration;
import com.chiangraipass.privacy.dto.PrivacyPreferencesResponse;
import com.chiangraipass.privacy.dto.PrivacyPreferencesResponse.Item;
import com.chiangraipass.privacy.model.PrivacyPurpose;
import com.chiangraipass.privacy.service.PrivacyPreferenceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PrivacyPreferenceController.class)
@Import({CorsConfiguration.class, SecurityConfiguration.class})
class PrivacyPreferenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PrivacyPreferenceService service;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private VisitorOAuth2UserService oauthUserService;

    private final UUID id = UUID.randomUUID();

    private RequestPostProcessor signedIn() {
        EmailVisitorPrincipal principal =
                new EmailVisitorPrincipal(new VisitorAccount(id, "visitor@example.com", "Visitor", "hash"));
        return authentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()));
    }

    private PrivacyPreferencesResponse response(boolean granted) {
        return new PrivacyPreferencesResponse(true, "2026-10", "https://example.com/privacy",
                List.of(new Item(PrivacyPurpose.MARKETING_EMAILS, granted, null),
                        new Item(PrivacyPurpose.USAGE_ANALYTICS, false, null)));
    }

    @Test
    void returnsTheCurrentChoices() throws Exception {
        given(service.view(id)).willReturn(response(false));

        mockMvc.perform(get("/api/v1/me/privacy-preferences").with(signedIn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.preferences[0].purpose").value("MARKETING_EMAILS"))
                .andExpect(jsonPath("$.preferences[0].granted").value(false));
    }

    @Test
    void savesChoices() throws Exception {
        given(service.save(any(), any())).willReturn(response(true));

        mockMvc.perform(post("/api/v1/me/privacy-preferences")
                        .with(signedIn()).with(csrf())
                        .contentType("application/json")
                        .content("{\"noticeVersion\":\"2026-10\",\"choices\":[{\"purpose\":\"MARKETING_EMAILS\",\"granted\":true}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferences[0].granted").value(true));
    }

    @Test
    void rejectsAMissingChoice() throws Exception {
        mockMvc.perform(post("/api/v1/me/privacy-preferences")
                        .with(signedIn()).with(csrf())
                        .contentType("application/json")
                        .content("{\"noticeVersion\":\"2026-10\",\"choices\":[{\"purpose\":\"MARKETING_EMAILS\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The choice is required."));

        verify(service, never()).save(any(), any());
    }

    @Test
    void rejectsAnUnknownPurpose() throws Exception {
        mockMvc.perform(post("/api/v1/me/privacy-preferences")
                        .with(signedIn()).with(csrf())
                        .contentType("application/json")
                        .content("{\"noticeVersion\":\"2026-10\",\"choices\":[{\"purpose\":\"SELL_DATA\",\"granted\":true}]}"))
                .andExpect(status().isBadRequest());

        verify(service, never()).save(any(), any());
    }

    @Test
    void requiresSignIn() throws Exception {
        mockMvc.perform(get("/api/v1/me/privacy-preferences")).andExpect(status().isUnauthorized());
    }
}