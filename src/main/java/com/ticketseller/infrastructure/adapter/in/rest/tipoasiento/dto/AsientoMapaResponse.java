package com.ticketseller.infrastructure.adapter.in.rest.tipoasiento.dto;

import java.util.UUID;

public record AsientoMapaResponse(
        UUID id,
        String fila,
        int columna,
        String numero,
        boolean existente,
        String estado
) {
}
