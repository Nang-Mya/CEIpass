package com.chiangraipass.profile.controller;

import com.chiangraipass.auth.model.VisitorIdentity;
import com.chiangraipass.profile.dto.MyPassResponse;
import com.chiangraipass.profile.service.MyPassService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class MyPassController {

    private final MyPassService myPass;

    public MyPassController(MyPassService myPass) {
        this.myPass = myPass;
    }

    @GetMapping("/pass")
    public ResponseEntity<MyPassResponse> pass(@AuthenticationPrincipal VisitorIdentity identity) {
        MyPassResponse body = new MyPassResponse(myPass.currentPass(identity.accountId()).orElse(null));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
}