package com.ticketseller.infrastructure.adapter.in.rest.dto;

import java.util.UUID;

public record ZonaResponse(
        UUID id,
        UUID recintoId,
        String nombre,
        Integer capacidad
) {
}

