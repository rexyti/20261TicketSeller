package com.ticketseller.infrastructure.adapter.in.rest.dto.promocion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AplicarCodigoRequest(
        @NotNull UUID ventaId,
        @NotBlank String codigo
) {
}

