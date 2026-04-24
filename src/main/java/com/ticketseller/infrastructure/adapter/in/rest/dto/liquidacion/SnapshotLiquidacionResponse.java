package com.ticketseller.infrastructure.adapter.in.rest.dto.liquidacion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SnapshotLiquidacionResponse(
        UUID eventoId,
        List<CondicionTicketResponse> condiciones,
        LocalDateTime timestampGeneracion
) {
}
