package com.ticketseller.infrastructure.adapter.in.rest.dto.promocion;

import java.math.BigDecimal;
import java.util.UUID;

public record AplicacionDescuentoResponse(
        UUID descuentoId,
        BigDecimal subtotal,
        BigDecimal montoDescuento,
        BigDecimal totalFinal,
        String descripcion
) {
}

