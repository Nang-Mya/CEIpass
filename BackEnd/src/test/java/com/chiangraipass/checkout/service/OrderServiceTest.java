package com.chiangraipass.checkout.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.chiangraipass.auth.entity.VisitorAccountEntity;
import com.chiangraipass.auth.repository.VisitorAccountJpaRepository;
import com.chiangraipass.catalog.entity.PassCatalogEntity;
import com.chiangraipass.catalog.repository.PassCatalogJpaRepository;
import com.chiangraipass.checkout.dto.CheckoutRequest;
import com.chiangraipass.pass.entity.UserPassEntity;
import com.chiangraipass.pass.repository.UserPassJpaRepository;
import com.chiangraipass.payment.entity.PaymentEntity;
import com.chiangraipass.payment.repository.PaymentJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private VisitorAccountJpaRepository accountRepository;

    @Mock
    private PassCatalogJpaRepository passRepository;

    @Mock
    private PaymentJpaRepository paymentRepository;

    @Mock
    private UserPassJpaRepository userPassRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void storesHolderNameAndTravelDatesOnPurchasedPass() {
        UUID accountId = UUID.randomUUID();
        String passId = "day-pass";
        VisitorAccountEntity account = new VisitorAccountEntity();
        PassCatalogEntity pass = new PassCatalogEntity();
        pass.setPassId(passId);
        pass.setPrice(new BigDecimal("100.00"));
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(passRepository.findById(passId)).thenReturn(Optional.of(pass));
        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> {
            PaymentEntity payment = invocation.getArgument(0);
            payment.setPaymentId(UUID.randomUUID());
            return payment;
        });
        when(userPassRepository.save(any(UserPassEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = startDate.plusDays(2);
        orderService.checkout(accountId, new CheckoutRequest(
                passId, "  Example Holder  ", startDate, endDate, 1));

        ArgumentCaptor<UserPassEntity> passCaptor = ArgumentCaptor.forClass(UserPassEntity.class);
        verify(userPassRepository).save(passCaptor.capture());
        assertEquals("Example Holder", passCaptor.getValue().getHolderName());
        assertEquals(startDate, passCaptor.getValue().getStartDate());
        assertEquals(endDate, passCaptor.getValue().getEndDate());
    }
}
