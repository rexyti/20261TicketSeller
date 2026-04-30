package com.ticketseller.application.promocion;

import com.ticketseller.domain.model.promocion.TipoDescuento;

import java.math.BigDecimal;
import java.util.UUID;

public record CrearDescuentoCommand(
        UUID promocionId,
        TipoDescuento tipo,
        BigDecimal valor,
        UUID zonaId,
        boolean acumulable
) {
}

