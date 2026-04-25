package com.ticketseller.infrastructure.adapter.in.rest.dto.liquidacion;

import java.math.BigDecimal;

public record CondicionTicketResponse(
        String condicion,
        long cantidad,
        BigDecimal valorTotal
) {
}
