package com.ticketseller.infrastructure.adapter.in.rest.dto.promocion;

import com.ticketseller.domain.model.promocion.TipoDescuento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CrearDescuentoRequest(
        @NotNull TipoDescuento tipo,
        @NotNull @DecimalMin(value = "0.01") BigDecimal valor,
        UUID zonaId,
        Boolean acumulable
) {
}

