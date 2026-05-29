package com.ticketseller.infrastructure.adapter.in.rest.asiento.dto;

import java.util.UUID;

public record AsientoResponse(
        UUID id,
        String fila,
        Integer columna,
        String numero,
        UUID zonaId,
        String estado
) {
}
