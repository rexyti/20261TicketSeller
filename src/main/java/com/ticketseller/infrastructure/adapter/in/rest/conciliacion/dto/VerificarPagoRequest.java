package com.ticketseller.infrastructure.adapter.in.rest.conciliacion.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record VerificarPagoRequest(
        @NotNull(message = "ventaId es obligatorio")
        UUID ventaId,
        @NotNull(message = "montoPasarela es obligatorio")
        @Positive(message = "montoPasarela debe ser mayor a 0")
        BigDecimal montoPasarela,
        String idExternoPasarela
) {
}
