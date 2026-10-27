package com.chiangraipass.profile.controller;

import java.util.Optional;
import java.util.UUID;

import com.chiangraipass.auth.model.EmailVisitorPrincipal;
import com.chiangraipass.auth.model.VisitorAccount;
import com.chiangraipass.auth.service.VisitorOAuth2UserService;
import com.chiangraipass.config.CorsConfiguration;
import com.chiangraipass.config.SecurityConfiguration;
import com.chiangraipass.profile.service.AvatarService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AvatarController.class)
@Import({CorsConfiguration.class, SecurityConfiguration.class})
class AvatarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AvatarService avatars;

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

    @Test
    void answersNoContentWhenThereIsNoPicture() throws Exception {
        given(avatars.find(id)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/me/avatar").with(signedIn()))
                .andExpect(status().isNoContent());
    }

    @Test
    void returnsThePictureAsAJpeg() throws Exception {
        byte[] picture = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0};
        given(avatars.find(id)).willReturn(Optional.of(picture));

        mockMvc.perform(get("/api/v1/me/avatar").with(signedIn()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().bytes(picture));
    }

    @Test
    void storesAnUploadedPicture() throws Exception {
        byte[] upload = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3};

        mockMvc.perform(put("/api/v1/me/avatar")
                        .with(signedIn()).with(csrf())
                        .contentType("image/jpeg")
                        .content(upload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profile picture updated."));

        verify(avatars).save(eq(id), aryEq(upload));
    }

    @Test
    void refusesAnUploadOverTwoMegabytes() throws Exception {
        mockMvc.perform(put("/api/v1/me/avatar")
                        .with(signedIn()).with(csrf())
                        .contentType("image/jpeg")
                        .content(new byte[AvatarService.MAX_UPLOAD_BYTES + 1]))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("too large")));

        verify(avatars, never()).save(any(), any());
    }

    @Test
    void requiresSignIn() throws Exception {
        mockMvc.perform(put("/api/v1/me/avatar").with(csrf()).contentType("image/jpeg").content(new byte[]{1}))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/me/avatar")).andExpect(status().isUnauthorized());
    }
}