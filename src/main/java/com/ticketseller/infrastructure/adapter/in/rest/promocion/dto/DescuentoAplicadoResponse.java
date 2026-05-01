package com.ticketseller.infrastructure.adapter.in.rest.promocion.dto;

import java.math.BigDecimal;

public record DescuentoAplicadoResponse(
        BigDecimal subtotalOriginal,
        BigDecimal montoDescuento,
        BigDecimal totalFinal
) {
}
