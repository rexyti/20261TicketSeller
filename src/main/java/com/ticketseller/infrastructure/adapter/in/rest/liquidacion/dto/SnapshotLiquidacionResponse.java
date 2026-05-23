package com.ticketseller.infrastructure.adapter.in.rest.liquidacion.dto;

import com.ticketseller.domain.model.recinto.CategoriaRecinto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SnapshotLiquidacionResponse(
        UUID eventoId,
        String nombreEvento,
        UUID recintoId,
        CategoriaRecinto tipoRecinto,
        List<CondicionTicketResponse> condiciones,
        LocalDateTime timestampGeneracion
) {
}
