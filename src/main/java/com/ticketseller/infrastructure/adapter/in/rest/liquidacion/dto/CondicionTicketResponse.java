package com.ticketseller.infrastructure.adapter.in.rest.liquidacion.dto;

import java.math.BigDecimal;

public record CondicionTicketResponse(
        String condicion,
        long cantidad,
        BigDecimal valorTotal
) {
}
