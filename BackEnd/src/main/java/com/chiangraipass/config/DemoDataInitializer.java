package com.chiangraipass.config;

import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.chiangraipass.auth.service.VisitorAccountService;

@Configuration
public class DemoDataInitializer {

    @Bean
    CommandLineRunner initDemoAccounts(
            VisitorAccountService accountService,
            PasswordEncoder passwordEncoder) {
        return args -> {
            // Demo account 1: for general testing
            String demoEmail1 = "demo@ceipass.com";
            if (accountService.findByEmail(demoEmail1).isEmpty()) {
                accountService.createEmailAccount(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        demoEmail1,
                        "Demo Visitor",
                        passwordEncoder.encode("password123456"),
                        "v1.0"
                );
            }

            // Demo account 2: for testing with a different user and switch to a different account
            String demoEmail2 = "visitor@chiangrai.com";
            if (accountService.findByEmail(demoEmail2).isEmpty()) {
                accountService.createEmailAccount(
                        UUID.fromString("22222222-2222-2222-2222-222222222222"),
                        demoEmail2,
                        "Somchai Traveler",
                        passwordEncoder.encode("chiangrai2026"),
                        "v1.0"
                );
            }
        };
    }
}