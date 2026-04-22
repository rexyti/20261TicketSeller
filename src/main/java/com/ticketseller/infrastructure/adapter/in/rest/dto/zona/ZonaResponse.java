package com.ticketseller.infrastructure.adapter.in.rest.dto.zona;

import java.util.UUID;

public record ZonaResponse(
                UUID id,
                UUID recintoId,
                String nombre,
                Integer capacidad) {
}
