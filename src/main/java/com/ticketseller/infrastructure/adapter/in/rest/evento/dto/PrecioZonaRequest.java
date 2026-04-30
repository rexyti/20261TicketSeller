package com.ticketseller.infrastructure.adapter.in.rest.evento.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record PrecioZonaRequest(
        @NotNull UUID zonaId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal precio
) {
}

