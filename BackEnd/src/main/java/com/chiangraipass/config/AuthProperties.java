package com.chiangraipass.config;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AuthProperties(Auth auth, Privacy privacy, OAuth oauth) {

    public record Auth(String frontendUrl, boolean registrationEnabled) {
    }

    public record Privacy(String noticeVersion, String noticeUrl) {
        public boolean isNoticePublished() {
            if (noticeVersion == null || noticeVersion.isBlank()
                    || "unpublished".equalsIgnoreCase(noticeVersion)
                    || noticeUrl == null || noticeUrl.isBlank()) {
                return false;
            }
            try {
                URI uri = URI.create(noticeUrl);
                return uri.isAbsolute()
                        && ("https".equalsIgnoreCase(uri.getScheme())
                        || ("http".equalsIgnoreCase(uri.getScheme())
                        && ("localhost".equalsIgnoreCase(uri.getHost())
                        || "127.0.0.1".equals(uri.getHost()))));
            } catch (IllegalArgumentException exception) {
                return false;
            }
        }
    }

    public record OAuth(boolean enabled, Provider google, Provider apple) {
        public record Provider(String clientId, String clientSecret) {
            public boolean isConfigured() {
                return clientId != null && !clientId.isBlank()
                        && clientSecret != null && !clientSecret.isBlank();
            }
        }
    }
}
