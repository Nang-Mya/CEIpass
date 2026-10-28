package com.chiangraipass.history.controller;

import java.util.List;

import com.chiangraipass.auth.model.VisitorIdentity;
import com.chiangraipass.history.dto.PassCardResponse;
import com.chiangraipass.history.service.PassesService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class PassesController {

    private final PassesService passesService;

    public PassesController(PassesService passesService) {
        this.passesService = passesService;
    }

    @GetMapping("/api/v1/me/passes")
    public List<PassCardResponse> passes(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof VisitorIdentity identity)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return passesService.passes(identity.accountId());
    }
}
