package com.ticketseller.infrastructure.adapter.in.rest.dto.promocion;

import com.ticketseller.domain.model.promocion.TipoDescuento;

import java.math.BigDecimal;
import java.util.UUID;

public record DescuentoResponse(
        UUID id,
        UUID promocionId,
        TipoDescuento tipo,
        BigDecimal valor,
        UUID zonaId,
        boolean acumulable
) {
}

