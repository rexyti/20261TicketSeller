package com.ticketseller.infrastructure.adapter.in.rest.dto.evento;

import com.ticketseller.domain.model.EstadoEvento;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventoResponse(
        UUID id,
        String nombre,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        String tipo,
        UUID recintoId,
        EstadoEvento estado,
        String motivoCancelacion
) {
}

