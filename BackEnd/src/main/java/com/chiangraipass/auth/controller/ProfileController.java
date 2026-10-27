package com.chiangraipass.auth.controller;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.chiangraipass.auth.dto.AccountResponse;
import com.chiangraipass.auth.dto.ChangePasswordRequest;
import com.chiangraipass.auth.dto.UpdateProfileRequest;
import com.chiangraipass.auth.model.EmailVisitorPrincipal;
import com.chiangraipass.auth.model.VisitorAccount;
import com.chiangraipass.auth.model.VisitorIdentity;
import com.chiangraipass.auth.service.LoginAttemptService;
import com.chiangraipass.auth.service.PasswordPolicy;
import com.chiangraipass.auth.service.VisitorAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Lets a signed-in visitor change their display name and password. */
@RestController
@RequestMapping("/api/v1/me")
public class ProfileController {

    private final VisitorAccountService accounts;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttempts;
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public ProfileController(
            VisitorAccountService accounts,
            PasswordEncoder passwordEncoder,
            LoginAttemptService loginAttempts) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.loginAttempts = loginAttempts;
    }

    @PatchMapping("/profile")
    public ResponseEntity<AccountResponse> updateProfile(
            @AuthenticationPrincipal VisitorIdentity identity,
            @Valid @RequestBody UpdateProfileRequest request) {
        String name = request.displayName().strip();
        if (name.chars().anyMatch(Character::isISOControl)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Name contains characters that are not allowed.");
        }
        if (!accounts.updateDisplayName(identity.accountId(), name)) {
            throw signInRequired();
        }
        VisitorAccount account = accounts.findById(identity.accountId()).orElseThrow(this::signInRequired);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(toResponse(account));
    }

    @PostMapping("/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal VisitorIdentity identity,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        VisitorAccount account = accounts.findById(identity.accountId()).orElseThrow(this::signInRequired);
        if (account.passwordHash() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This account has no password to change.");
        }
        if (account.isLocked(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.LOCKED,
                    "Too many incorrect passwords. The account is locked for "
                            + LoginAttemptService.LOCK_DURATION.toMinutes() + " minutes. Try again later.");
        }
        PasswordPolicy.validate(request.newPassword()).ifPresent(problem -> {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, problem);
        });
        if (request.currentPassword().equals(request.newPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The new password must be different from the current password.");
        }
        if (!passwordEncoder.matches(request.currentPassword(), account.passwordHash())) {
            loginAttempts.recordFailure(account.email());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your current password is incorrect.");
        }

        accounts.changePassword(account.accountId(), passwordEncoder.encode(request.newPassword()));
        keepThisSignInAlive(account.accountId(), httpRequest, httpResponse);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of("message", "Password updated. Your other devices have been signed out."));
    }

    /**
     * Changing the password rotates the account's security token, which signs out every session.
     * Re-issue this session's sign-in with the new token so the person changing the password
     * stays signed in on this device.
     */
    private void keepThisSignInAlive(UUID accountId, HttpServletRequest request, HttpServletResponse response) {
        accounts.findById(accountId).ifPresent(refreshed -> {
            EmailVisitorPrincipal principal = new EmailVisitorPrincipal(refreshed);
            SecurityContext context = SecurityContextHolder.getContextHolderStrategy().createEmptyContext();
            context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                    principal, null, principal.getAuthorities()));
            SecurityContextHolder.getContextHolderStrategy().setContext(context);
            securityContextRepository.saveContext(context, request, response);
        });
    }

    private AccountResponse toResponse(VisitorAccount account) {
        return new AccountResponse(
                account.accountId(), account.email(), account.displayName(), account.passwordHash() != null);
    }

    private ResponseStatusException signInRequired() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in is required.");
    }
}