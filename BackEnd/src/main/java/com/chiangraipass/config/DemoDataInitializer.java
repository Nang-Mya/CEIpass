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
        };
    }
}