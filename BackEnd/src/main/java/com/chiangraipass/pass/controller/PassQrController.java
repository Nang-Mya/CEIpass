package com.chiangraipass.pass.controller;

import com.chiangraipass.auth.model.VisitorIdentity;
import com.chiangraipass.pass.dto.QrPassResponse;
import com.chiangraipass.pass.service.PassQrService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/me")
public class PassQrController {

    private final PassQrService passQrService;

    public PassQrController(PassQrService passQrService) {
        this.passQrService = passQrService;
    }

    @GetMapping("/qr")
    public ResponseEntity<QrPassResponse> getMyQr(@AuthenticationPrincipal VisitorIdentity identity) {
        if (identity == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in is required.");
        }
        QrPassResponse response = passQrService.issueQrForAccount(identity.accountId());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }
}