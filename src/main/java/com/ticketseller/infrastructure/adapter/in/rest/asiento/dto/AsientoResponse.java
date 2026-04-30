package com.ticketseller.infrastructure.adapter.in.rest.asiento.dto;

import com.ticketseller.domain.model.asiento.EstadoAsiento;

import java.util.UUID;

public record AsientoResponse(
        UUID id,
        String fila,
        Integer columna,
        String numero,
        UUID zonaId,
        EstadoAsiento estado
) {
}
