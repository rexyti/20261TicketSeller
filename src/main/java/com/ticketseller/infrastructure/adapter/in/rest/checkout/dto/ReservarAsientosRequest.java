package com.ticketseller.infrastructure.adapter.in.rest.checkout.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReservarAsientosRequest(
        @NotNull UUID compradorId,
        @NotNull UUID eventoId,
        @NotNull UUID zonaId,
        @NotNull @Min(1) Integer cantidad,
        Boolean esCortesia
) {
}

