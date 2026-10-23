package com.chiangraipass.config;

import java.util.ArrayList;
import java.util.List;

import com.chiangraipass.config.AuthProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;

@Configuration
public class OAuthClientConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "app.oauth", name = "enabled", havingValue = "true")
    ClientRegistrationRepository clientRegistrationRepository(AuthProperties properties) {
        List<ClientRegistration> registrations = new ArrayList<>();
        AuthProperties.OAuth.Provider google = properties.oauth().google();
        AuthProperties.OAuth.Provider apple = properties.oauth().apple();

        if (google != null && google.isConfigured()) {
            registrations.add(CommonOAuth2Provider.GOOGLE.getBuilder("google")
                    .clientId(google.clientId())
                    .clientSecret(google.clientSecret())
                    .scope(OidcScopes.OPENID, OidcScopes.EMAIL, OidcScopes.PROFILE)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .build());
        }

        if (apple != null && apple.isConfigured()) {
            registrations.add(ClientRegistration.withRegistrationId("apple")
                    .clientName("Apple")
                    .clientId(apple.clientId())
                    .clientSecret(apple.clientSecret())
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .scope(OidcScopes.OPENID, OidcScopes.EMAIL, "name")
                    .authorizationUri("https://appleid.apple.com/auth/authorize")
                    .tokenUri("https://appleid.apple.com/auth/token")
                    .jwkSetUri("https://appleid.apple.com/auth/keys")
                    .userNameAttributeName("sub")
                    .issuerUri("https://appleid.apple.com")
                    .build());
        }

        if (registrations.isEmpty()) {
            throw new IllegalStateException(
                    "OAUTH_ENABLED=true requires complete Google and/or Apple client credentials.");
        }
        return new InMemoryClientRegistrationRepository(registrations);
    }
}
