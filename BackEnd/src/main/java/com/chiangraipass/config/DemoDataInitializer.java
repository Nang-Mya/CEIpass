package com.chiangraipass.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.chiangraipass.auth.entity.VisitorAccountEntity;
import com.chiangraipass.auth.repository.VisitorAccountJpaRepository;
import com.chiangraipass.auth.service.VisitorAccountService;
import com.chiangraipass.catalog.entity.PassCatalogEntity;
import com.chiangraipass.catalog.repository.PassCatalogJpaRepository;
import com.chiangraipass.pass.entity.UserPassEntity;
import com.chiangraipass.pass.repository.UserPassJpaRepository;
import com.chiangraipass.payment.entity.PaymentEntity;
import com.chiangraipass.payment.repository.PaymentJpaRepository;

@Configuration
public class DemoDataInitializer {

    @Bean
    CommandLineRunner initDemoAccounts(
            VisitorAccountService accountService,
            VisitorAccountJpaRepository accountRepository,
            PassCatalogJpaRepository passRepository,
            PaymentJpaRepository paymentRepository,
            UserPassJpaRepository userPassRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            UUID demoId1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
            String demoEmail1 = "demo@ceipass.com";
            // Create a demo account if it doesn't exist
            if (accountService.findByEmail(demoEmail1).isEmpty()) {
                accountService.createEmailAccount(
                        demoId1,
                        demoEmail1,
                        "Demo Visitor",
                        passwordEncoder.encode("password123456"),
                        "v1.0"
                );
            }
            // Create a demo pass and payment
            VisitorAccountEntity account = accountRepository.findById(demoId1).orElse(null);
            PassCatalogEntity dayPass = passRepository.findById("day-pass").orElse(null);
            // check if userPassRepository is empty to avoid creating multiple demo passes
            if (account != null && dayPass != null && userPassRepository.count() == 0) {
                // Create a demo payment
                PaymentEntity payment = new PaymentEntity();
                payment.setAccount(account);
                payment.setPass(dayPass);
                payment.setPaymentMethod("PROTOTYPE_CHECKOUT");
                payment.setPaymentStatus("SUCCESS");
                payment.setAmount(new BigDecimal("299.00"));
                payment = paymentRepository.save(payment);

                // Create a demo user pass
                UserPassEntity userPass = new UserPassEntity();
                userPass.setAccount(account);
                userPass.setPass(dayPass);
                userPass.setPayment(payment);
                userPass.setHolderName(account.getDisplayName());
                userPass.setStartDate(LocalDate.now()); // start today
                userPass.setEndDate(LocalDate.now().plusDays(1)); // end tomorrow
                userPass.setStatus("ACTIVE"); 
                userPassRepository.save(userPass);
            }   
        };
    }
}