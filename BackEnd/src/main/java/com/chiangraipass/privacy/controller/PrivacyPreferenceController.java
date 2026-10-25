package com.chiangraipass.privacy.controller;

import com.chiangraipass.auth.model.VisitorIdentity;
import com.chiangraipass.privacy.dto.PrivacyPreferencesRequest;
import com.chiangraipass.privacy.dto.PrivacyPreferencesResponse;
import com.chiangraipass.privacy.service.PrivacyPreferenceService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/privacy-preferences")
public class PrivacyPreferenceController {

    private final PrivacyPreferenceService service;

    public PrivacyPreferenceController(PrivacyPreferenceService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<PrivacyPreferencesResponse> current(@AuthenticationPrincipal VisitorIdentity identity) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.view(identity.accountId()));
    }

    @PostMapping
    public ResponseEntity<PrivacyPreferencesResponse> save(
            @AuthenticationPrincipal VisitorIdentity identity,
            @Valid @RequestBody PrivacyPreferencesRequest request) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.save(identity.accountId(), request));
    }
}