package com.ticketseller.infrastructure.adapter.in.rest.dto.postventa;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CancelacionResponse(
        List<UUID> ticketsCancelados,
        UUID reembolsoId,
        BigDecimal montoPendiente
) {
}

