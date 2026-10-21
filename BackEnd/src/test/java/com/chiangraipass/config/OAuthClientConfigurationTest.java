package com.chiangraipass.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OAuthClientConfigurationTest {

    private final OAuthClientConfiguration configuration = new OAuthClientConfiguration();

    @Test
    void createsGoogleAndAppleOpenIdRegistrationsWhenCredentialsAreConfigured() {
        AuthProperties properties = properties(
                new AuthProperties.OAuth.Provider("google-client", "google-secret"),
                new AuthProperties.OAuth.Provider("apple-service", "apple-client-secret-jwt"));

        ClientRegistrationRepository registrations = configuration.clientRegistrationRepository(properties);
        ClientRegistration google = registrations.findByRegistrationId("google");
        ClientRegistration apple = registrations.findByRegistrationId("apple");

        assertNotNull(google);
        assertEquals("google-client", google.getClientId());
        assertEquals("https://accounts.google.com/o/oauth2/v2/auth", google.getProviderDetails().getAuthorizationUri());
        assertNotNull(apple);
        assertEquals("apple-service", apple.getClientId());
        assertEquals("https://appleid.apple.com/auth/authorize", apple.getProviderDetails().getAuthorizationUri());
        assertEquals("https://appleid.apple.com/auth/keys", apple.getProviderDetails().getJwkSetUri());
        assertEquals("https://appleid.apple.com", apple.getProviderDetails().getIssuerUri());
    }

    @Test
    void rejectsOauthEnabledWithoutAnyCompleteProviderConfiguration() {
        AuthProperties properties = properties(
                new AuthProperties.OAuth.Provider("google-client", ""),
                new AuthProperties.OAuth.Provider("", ""));

        assertThrows(
                IllegalStateException.class,
                () -> configuration.clientRegistrationRepository(properties));
    }

    private AuthProperties properties(
            AuthProperties.OAuth.Provider google,
            AuthProperties.OAuth.Provider apple) {
        return new AuthProperties(
                new AuthProperties.Auth("http://localhost:5500", false),
                new AuthProperties.Privacy("unpublished", ""),
                new AuthProperties.OAuth(true, google, apple));
    }
}
