package com.chiangraipass.checkout.service;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.chiangraipass.auth.entity.VisitorAccountEntity;
import com.chiangraipass.auth.repository.VisitorAccountJpaRepository;
import com.chiangraipass.catalog.entity.PassCatalogEntity;
import com.chiangraipass.catalog.repository.PassCatalogJpaRepository;
import com.chiangraipass.checkout.dto.CheckoutRequest;
import com.chiangraipass.pass.entity.UserPassEntity;
import com.chiangraipass.pass.repository.UserPassJpaRepository;
import com.chiangraipass.payment.entity.PaymentEntity;
import com.chiangraipass.payment.repository.PaymentJpaRepository;

@Service
public class OrderService {

    private final VisitorAccountJpaRepository accountRepository;
    private final PassCatalogJpaRepository passRepository;
    private final PaymentJpaRepository paymentRepository;
    private final UserPassJpaRepository userPassRepository;

    public OrderService(VisitorAccountJpaRepository accountRepository,
                        PassCatalogJpaRepository passRepository,
                        PaymentJpaRepository paymentRepository,
                        UserPassJpaRepository userPassRepository) {
        this.accountRepository = accountRepository;
        this.passRepository = passRepository;
        this.paymentRepository = paymentRepository;
        this.userPassRepository = userPassRepository;
    }

    @Transactional
    public Map<String, Object> checkout(UUID accountId, CheckoutRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date cannot be before start date.");
        }

        long days = ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1;

        VisitorAccountEntity account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found."));

        PassCatalogEntity pass = passRepository.findById(request.passId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pass catalog item not found."));

        BigDecimal totalAmount = pass.getPrice()
                .multiply(BigDecimal.valueOf(days))
                .multiply(BigDecimal.valueOf(request.quantity()));

        // ให้ @GeneratedValue และ @PrePersist ของเพื่อนจัดการ ID และ Timestamp ให้อัตโนมัติ
        PaymentEntity payment = new PaymentEntity();
        payment.setAccount(account);
        payment.setPass(pass);
        payment.setPaymentMethod("PROTOTYPE_CHECKOUT");
        payment.setPaymentStatus("SUCCESS");
        payment.setAmount(totalAmount);
        payment = paymentRepository.save(payment);

        for (int i = 0; i < request.quantity(); i++) {
            UserPassEntity userPass = new UserPassEntity();
            userPass.setAccount(account);
            userPass.setPass(pass);
            userPass.setPayment(payment);
            userPass.setStartDate(request.startDate());
            userPass.setEndDate(request.endDate());
            userPass.setStatus("ACTIVE");
            userPassRepository.save(userPass);
        }

        return Map.of(
                "orderId", payment.getPaymentId(),
                "status", payment.getPaymentStatus(),
                "amount", payment.getAmount()
        );
    }
}