package com.ticketseller.infrastructure.adapter.in.rest.dto.tipoasiento;

import java.util.UUID;

public record AsientoMapaResponse(
        UUID id,
        int fila,
        int columna,
        String numero,
        boolean existente,
        String estado
) {
}
