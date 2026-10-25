package com.chiangraipass.profile.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.chiangraipass.auth.model.EmailVisitorPrincipal;
import com.chiangraipass.auth.model.VisitorAccount;
import com.chiangraipass.auth.service.VisitorOAuth2UserService;
import com.chiangraipass.config.CorsConfiguration;
import com.chiangraipass.config.SecurityConfiguration;
import com.chiangraipass.profile.dto.MyPassResponse.BenefitUsage;
import com.chiangraipass.profile.dto.MyPassResponse.PassDetails;
import com.chiangraipass.profile.service.MyPassService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MyPassController.class)
@Import({CorsConfiguration.class, SecurityConfiguration.class})
class MyPassControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MyPassService myPass;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private VisitorOAuth2UserService oauthUserService;

    private final UUID id = UUID.randomUUID();

    private org.springframework.test.web.servlet.request.RequestPostProcessor signedIn() {
        EmailVisitorPrincipal principal =
                new EmailVisitorPrincipal(new VisitorAccount(id, "visitor@example.com", "Visitor", "hash"));
        return authentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()));
    }

    @Test
    void returnsThePassWithItsBenefits() throws Exception {
        given(myPass.currentPass(id)).willReturn(Optional.of(new PassDetails(
                "Explorer Pass", "ACTIVE", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3),
                List.of(new BenefitUsage("Wat Rong Khun", "Free entry", 1, 1)))));

        mockMvc.perform(get("/api/v1/me/pass").with(signedIn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pass.passName").value("Explorer Pass"))
                .andExpect(jsonPath("$.pass.status").value("ACTIVE"))
                .andExpect(jsonPath("$.pass.benefits[0].used").value(1))
                .andExpect(jsonPath("$.pass.benefits[0].limit").value(1));
    }

    @Test
    void returnsNullWhenTheVisitorHasNoPass() throws Exception {
        given(myPass.currentPass(id)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/me/pass").with(signedIn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pass").doesNotExist());
    }

    @Test
    void requiresSignIn() throws Exception {
        mockMvc.perform(get("/api/v1/me/pass")).andExpect(status().isUnauthorized());
    }
}