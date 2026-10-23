package com.chiangraipass.auth.dto;

import java.util.List;

public record AuthConfiguration(
        boolean registrationEnabled,
        List<String> oauthProviders,
        String privacyNoticeUrl) {
}
