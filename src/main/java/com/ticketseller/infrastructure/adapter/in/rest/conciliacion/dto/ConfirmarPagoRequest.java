package com.ticketseller.infrastructure.adapter.in.rest.conciliacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record ConfirmarPagoRequest(
        @NotNull(message = "ventaId es obligatorio")
        UUID ventaId,
        @NotBlank(message = "idExternoPasarela es obligatorio")
        String idExternoPasarela,
        @NotNull(message = "montoPasarela es obligatorio")
        @Positive(message = "montoPasarela debe ser mayor a 0")
        BigDecimal montoPasarela
) {
}
