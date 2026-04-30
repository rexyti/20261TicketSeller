package com.ticketseller.application.promocion;

import java.math.BigDecimal;
import java.util.UUID;

public record AplicacionDescuentoResultado(
        BigDecimal subtotal,
        BigDecimal montoDescuento,
        BigDecimal totalFinal,
        UUID descuentoId,
        String descripcion
) {
}

