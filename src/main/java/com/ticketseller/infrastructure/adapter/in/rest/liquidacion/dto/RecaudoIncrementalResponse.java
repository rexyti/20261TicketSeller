package com.ticketseller.infrastructure.adapter.in.rest.liquidacion.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record RecaudoIncrementalResponse(
        UUID eventoId,
        BigDecimal recaudoRegular,
        BigDecimal recaudoCortesia,
        BigDecimal cancelaciones,
        BigDecimal recaudoNeto,
        LocalDateTime timestamp
) {
}
