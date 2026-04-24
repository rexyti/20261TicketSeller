package com.ticketseller.infrastructure.adapter.in.rest.dto.checkout;

import jakarta.validation.constraints.NotBlank;

public record ProcesarPagoRequest(
        @NotBlank String metodoPago,
        String ip
) {
}

