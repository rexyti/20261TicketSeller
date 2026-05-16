package com.ticketseller.infrastructure.adapter.in.rest.liquidacion.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TicketResumenResponse(
        UUID ticketId,
        BigDecimal precio
) {
}
