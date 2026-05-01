package com.ticketseller.infrastructure.adapter.in.rest.promocion.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CrearCodigosRequest(
        @NotNull @Min(1) Integer cantidad,
        Integer usosMaximosPorCodigo,
        String prefijo,
        @NotNull LocalDateTime fechaFin
) {
}
