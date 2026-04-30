package com.ticketseller.infrastructure.adapter.in.rest.checkout.dto;

import jakarta.validation.constraints.NotBlank;

public record ProcesarPagoRequest(
        @NotBlank String metodoPago,
        String ip
) {
}

