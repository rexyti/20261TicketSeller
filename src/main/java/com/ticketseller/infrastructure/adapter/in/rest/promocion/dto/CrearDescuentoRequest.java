package com.ticketseller.infrastructure.adapter.in.rest.promocion.dto;

import com.ticketseller.domain.model.promocion.TipoDescuento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CrearDescuentoRequest(
        @NotNull TipoDescuento tipo,
        @NotNull @Positive BigDecimal valor,
        UUID zonaId,
        boolean acumulable
) {
}
