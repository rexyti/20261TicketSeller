package com.ticketseller.infrastructure.adapter.in.rest.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ReservarAsientosRequest(
        UUID eventoId,
        UUID compradorId,
        List<UUID> ticketIds,
        BigDecimal total
) {}
