package com.ticketseller.infrastructure.adapter.in.rest.evento.dto;

import com.ticketseller.domain.model.evento.EstadoEvento;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventoResponse(
        UUID id,
        String nombre,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        String tipo,
        UUID recintoId,
        EstadoEvento estado
) {
}

