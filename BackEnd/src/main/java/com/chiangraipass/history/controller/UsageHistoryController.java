package com.chiangraipass.history.controller;

import java.util.List;

import com.chiangraipass.auth.model.VisitorIdentity;
import com.chiangraipass.history.dto.UsageHistoryItem;
import com.chiangraipass.history.service.UsageHistoryService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class UsageHistoryController {

    private final UsageHistoryService usageHistoryService;

    public UsageHistoryController(UsageHistoryService usageHistoryService) {
        this.usageHistoryService = usageHistoryService;
    }

    @GetMapping("/api/v1/me/usage-history")
    public List<UsageHistoryItem> usageHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "50") int limit) {
        if (authentication == null || !(authentication.getPrincipal() instanceof VisitorIdentity identity)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return usageHistoryService.recentUsage(identity.accountId(), limit);
    }
}
