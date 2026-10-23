package com.chiangraipass.auth.service;

import java.util.Locale;
import java.util.UUID;

import com.chiangraipass.auth.model.OAuthVisitorPrincipal;
import com.chiangraipass.auth.model.VisitorAccount;
import com.chiangraipass.config.AuthProperties;
import com.chiangraipass.config.OAuthPrivacyNoticeContext;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
public class VisitorOAuth2UserService extends OidcUserService {

    private final VisitorAccountService accountService;
    private final AuthProperties authProperties;

    public VisitorOAuth2UserService(
            VisitorAccountService accountService,
            AuthProperties authProperties) {
        this.accountService = accountService;
        this.authProperties = authProperties;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser providerUser = super.loadUser(userRequest);
        String email = providerUser.getEmail();
        Object emailVerified = providerUser.getClaim("email_verified");
        String provider = userRequest.getClientRegistration().getRegistrationId().toLowerCase(Locale.ROOT);

        if (email == null || !isVerified(emailVerified)) {
            throw oauthError("The identity provider did not return a verified email address.");
        }

        VisitorAccount account = accountService.findByExternalIdentity(provider, providerUser.getSubject())
                .orElseGet(() -> createNewOAuthAccount(providerUser, provider, email));
        return new OAuthVisitorPrincipal(account, providerUser);
    }

    private boolean isVerified(Object emailVerified) {
        return Boolean.TRUE.equals(emailVerified)
                || "true".equalsIgnoreCase(String.valueOf(emailVerified));
    }

    private VisitorAccount createNewOAuthAccount(OidcUser providerUser, String provider, String email) {
        if (!authProperties.auth().registrationEnabled() || !authProperties.privacy().isNoticePublished()) {
            throw oauthError("New account creation is disabled by the service operator.");
        }
        if (!hasAcknowledgedPrivacyNotice()) {
            throw oauthError("Acknowledge the published privacy notice before creating an account.");
        }

        String name = providerUser.getFullName();
        if (name == null || name.isBlank()) {
            name = email.substring(0, email.indexOf('@'));
        }

        if (accountService.findByEmail(email).isPresent()) {
            throw oauthError("This email already has an account. Sign in with that account before linking providers.");
        }

        try {
            return accountService.createOAuthAccount(
                    UUID.randomUUID(),
                    email,
                    name.strip(),
                    provider,
                    providerUser.getSubject(),
                    authProperties.privacy().noticeVersion());
        } catch (org.springframework.dao.DataIntegrityViolationException error) {
            return accountService.findByExternalIdentity(provider, providerUser.getSubject())
                    .orElseThrow(() -> oauthError("The provider account could not be linked."));
        }
    }

    private boolean hasAcknowledgedPrivacyNotice() {
        return authProperties.privacy().noticeVersion().equals(
                OAuthPrivacyNoticeContext.currentNoticeVersion());
    }

    private OAuth2AuthenticationException oauthError(String message) {
        return new OAuth2AuthenticationException(new OAuth2Error("login_not_allowed"), message);
    }
}
