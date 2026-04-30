package com.ticketseller.infrastructure.adapter.in.rest.recinto.dto;

import java.util.List;
import java.util.UUID;

public record RecintoEstructuraResponse(
    UUID recintoId,
    List<BloqueResponse> bloques
) {
    public record BloqueResponse(
        String nombre,
        List<ZonaResponse> zonas
    ) {}

    public record ZonaResponse(
        String nombre,
        String categoria,
        String coordenadaAcceso
    ) {}
}
