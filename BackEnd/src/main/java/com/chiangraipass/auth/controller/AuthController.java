package com.chiangraipass.auth.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.chiangraipass.auth.dto.AccountResponse;
import com.chiangraipass.auth.dto.AuthConfiguration;
import com.chiangraipass.auth.dto.OAuthPreparationRequest;
import com.chiangraipass.auth.dto.RegistrationRequest;
import com.chiangraipass.auth.dto.ResendVerificationRequest;
import com.chiangraipass.auth.dto.VerifyEmailRequest;
import com.chiangraipass.auth.model.VisitorIdentity;
import com.chiangraipass.auth.service.PasswordPolicy;
import com.chiangraipass.auth.service.VisitorAccountService;
import com.chiangraipass.auth.service.EmailVerificationService;
import com.chiangraipass.config.AuthProperties;
import com.chiangraipass.config.OAuthPrivacyNoticeContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.chiangraipass.auth.dto.AccountResponse;
import com.chiangraipass.auth.dto.AuthConfiguration;
import com.chiangraipass.auth.dto.OAuthPreparationRequest;
import com.chiangraipass.auth.dto.RegistrationRequest;
import com.chiangraipass.auth.model.VisitorIdentity;
import com.chiangraipass.auth.service.VisitorAccountService;
import com.chiangraipass.config.AuthProperties;
import com.chiangraipass.config.OAuthPrivacyNoticeContext;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final VisitorAccountService accountService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;
    private final EmailVerificationService emailVerification;

    public AuthController(
            VisitorAccountService accountService,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
            AuthProperties authProperties,
            EmailVerificationService emailVerification) {
        this.accountService = accountService;
        this.passwordEncoder = passwordEncoder;
        this.authProperties = authProperties;
        this.emailVerification = emailVerification;
    }

    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> csrf(CsrfToken csrfToken) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of("token", csrfToken != null ? csrfToken.getToken() : "csrf-disabled"));
                // .body(Map.of("token", csrfToken.getToken()));
    }

    @GetMapping("/config")
    public AuthConfiguration configuration() {
        AuthProperties.OAuth oauth = authProperties.oauth();
        List<String> providers = oauth == null || !oauth.enabled()
                ? List.of()
                : java.util.stream.Stream.of(
                                oauth.google() != null && oauth.google().isConfigured() ? "google" : null,
                                oauth.apple() != null && oauth.apple().isConfigured() ? "apple" : null)
                        .filter(java.util.Objects::nonNull)
                        .toList();
        return new AuthConfiguration(
                registrationEnabled(),
                providers,
                authProperties.privacy().isNoticePublished()
                        ? authProperties.privacy().noticeUrl()
                        : null);
    }

    @GetMapping("/me")
    public ResponseEntity<AccountResponse> currentAccount(Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof VisitorIdentity identity)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in is required.");
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new AccountResponse(identity.accountId(), identity.email(), identity.displayName()));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegistrationRequest request) {
        ensureRegistrationEnabled();
        if (!request.privacyNoticeAcknowledged()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Acknowledge the published privacy notice before creating an account.");
        }
            PasswordPolicy.validate(request.password()).ifPresent(problem -> {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, problem);
        });

        String email = VisitorAccountService.normalizeEmail(request.email());
        if (accountService.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account already exists for this email.");
        }

        UUID accountId = UUID.randomUUID();
        String displayName = request.fullName().strip();
        try {
            accountService.createEmailAccount(
                    accountId,
                    email,
                    displayName,
                    passwordEncoder.encode(request.password()),
                    authProperties.privacy().noticeVersion());
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account already exists for this email.");
        }

        emailVerification.sendVerification(accountId, email, displayName);

        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(Map.of("message",
                        "Account created. Check your email and open the verification link before signing in."));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        if (!emailVerification.verify(request.token())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This verification link is invalid or has expired. "
                            + "Sign in with your email and password to request a new one.");
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of("message", "Email verified. You can now sign in."));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {
        emailVerification.resend(request.email());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of("message",
                        "If that email belongs to an account that still needs verification, "
                                + "a new link has been sent."));
    }

    @PostMapping("/oauth/prepare")
    public ResponseEntity<Map<String, String>> prepareOAuth(
            @Valid @RequestBody OAuthPreparationRequest request,
            HttpServletRequest servletRequest) {
        String provider = request.provider().toLowerCase(java.util.Locale.ROOT);
        AuthProperties.OAuth oauth = authProperties.oauth();
        boolean enabled = oauth != null && oauth.enabled()
                && ("google".equals(provider) && oauth.google() != null && oauth.google().isConfigured()
                || "apple".equals(provider) && oauth.apple() != null && oauth.apple().isConfigured());
        if (!enabled) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "That sign-in provider is not configured.");
        }

        var session = servletRequest.getSession(true);
        if (authProperties.privacy().isNoticePublished() && request.privacyNoticeAcknowledged()) {
            session.setAttribute(
                    OAuthPrivacyNoticeContext.SESSION_KEY, authProperties.privacy().noticeVersion());
        } else {
            session.removeAttribute(OAuthPrivacyNoticeContext.SESSION_KEY);
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of("authorizationUrl", "/oauth2/authorization/" + provider));
    }

    private void ensureRegistrationEnabled() {
        if (!registrationEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Account creation is disabled until the operator enables it and publishes a reviewed privacy notice.");
        }
    }

    private boolean registrationEnabled() {
        return authProperties.auth().registrationEnabled()
                && authProperties.privacy().isNoticePublished();
    }

}
