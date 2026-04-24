package com.ticketseller.infrastructure.adapter.in.rest.dto;

import com.ticketseller.domain.model.EstadoAsiento;

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
