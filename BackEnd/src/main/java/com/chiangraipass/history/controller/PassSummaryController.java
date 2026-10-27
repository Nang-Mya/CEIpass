package com.chiangraipass.history.controller;

import com.chiangraipass.auth.model.VisitorIdentity;
import com.chiangraipass.history.dto.PassSummaryResponse;
import com.chiangraipass.history.service.PassSummaryService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class PassSummaryController {

    private final PassSummaryService passSummaryService;

    public PassSummaryController(PassSummaryService passSummaryService) {
        this.passSummaryService = passSummaryService;
    }

    @GetMapping("/api/v1/me/pass-summary")
    public PassSummaryResponse passSummary(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof VisitorIdentity identity)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return passSummaryService.summary(identity.accountId());
    }
}
