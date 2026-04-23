package com.ticketseller.infrastructure.adapter.in.rest.dto.evento;

import java.time.LocalDateTime;
import java.util.UUID;

public record EditarEventoRequest(
        String nombre,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        String tipo,
        UUID recintoId
) {
}

