package com.chiangraipass.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chiangraipass.auth.service.VisitorOAuth2UserService;
import com.chiangraipass.config.CorsConfiguration;
import com.chiangraipass.config.SecurityConfiguration;

@WebMvcTest(HealthController.class)
@Import({CorsConfiguration.class, SecurityConfiguration.class})
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private VisitorOAuth2UserService oauthUserService;

    @Test
    void returnsApiHealth() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.service").value("cei-pass-api"));
    }

    @Test
    void allowsTheConfiguredLocalFrontendOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/health")
                        .header("Origin", "http://localhost:8080")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8080"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void allowsCsrfHeaderForCredentialedFrontendRequests() throws Exception {
        mockMvc.perform(options("/api/v1/auth/register")
                        .header("Origin", "http://localhost:8080")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,x-xsrf-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8080"))
                .andExpect(header().string(
                        "Access-Control-Allow-Headers",
                        org.hamcrest.Matchers.containsStringIgnoringCase("X-XSRF-TOKEN")));
    }

    @Test
    void rejectsAnUnconfiguredFrontendOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/health")
                        .header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
