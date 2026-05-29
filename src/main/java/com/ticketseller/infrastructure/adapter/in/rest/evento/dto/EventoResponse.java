package com.ticketseller.infrastructure.adapter.in.rest.evento.dto;

import com.ticketseller.domain.model.evento.EstadoEvento;
import com.ticketseller.domain.model.evento.TipoEvento;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventoResponse(
        UUID id,
        String nombre,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        TipoEvento tipo,
        UUID recintoId,
        String nombreRecinto,
        EstadoEvento estado,
        boolean reingresoHabilitado
) {
}
