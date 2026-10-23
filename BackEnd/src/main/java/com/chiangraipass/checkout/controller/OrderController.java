// for open API
package com.chiangraipass.checkout.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chiangraipass.auth.model.VisitorIdentity;
import com.chiangraipass.checkout.dto.CheckoutRequest;
import com.chiangraipass.checkout.service.OrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/me/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<?> createOrder(
            @AuthenticationPrincipal VisitorIdentity identity,
            @Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.ok(orderService.checkout(identity.accountId(), request));
    }
}