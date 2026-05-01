package com.ticketseller.application.promocion;

import java.math.BigDecimal;

public record DescuentoAplicado(BigDecimal subtotalOriginal, BigDecimal montoDescuento, BigDecimal totalFinal) {
}
