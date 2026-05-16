package com.ticketseller.infrastructure.adapter.in.rest.liquidacion.dto;

import java.math.BigDecimal;
import java.util.List;

public record CondicionTicketResponse(
        String condicion,
        long cantidad,
        BigDecimal valorTotal,
        List<TicketResumenResponse> tickets
) {
}
