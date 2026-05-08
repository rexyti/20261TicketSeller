package com.ticketseller.infrastructure.adapter.in.rest.recinto.dto;

import java.util.List;
import java.util.UUID;

public record RecintoEstructuraResponse(
    UUID recintoId,
    List<ZonaResponse> bloques
) {

    public record ZonaResponse(
        String nombre,
        List<String> compuertas
    ) {}
}
