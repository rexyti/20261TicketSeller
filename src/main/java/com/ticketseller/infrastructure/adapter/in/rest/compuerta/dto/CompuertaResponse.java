package com.ticketseller.infrastructure.adapter.in.rest.compuerta.dto;

import java.util.UUID;

public record CompuertaResponse(
        UUID id,
        UUID recintoId,
        UUID zonaId,
        String nombre,
        boolean esGeneral
) {
}

