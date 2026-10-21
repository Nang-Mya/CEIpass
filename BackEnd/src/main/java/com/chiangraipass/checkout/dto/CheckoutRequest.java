// for receiving checkout request from frontend
package com.chiangraipass.checkout.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        @NotBlank String passId,
        @NotNull @FutureOrPresent LocalDate startDate,
        @NotNull LocalDate endDate,
        @Min(1) int quantity
) {
}