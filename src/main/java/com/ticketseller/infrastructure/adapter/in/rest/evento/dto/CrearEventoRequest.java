package com.ticketseller.infrastructure.adapter.in.rest.evento.dto;

import com.ticketseller.domain.model.evento.TipoEvento;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record CrearEventoRequest(
        @NotNull String nombre,
        @NotNull @Future LocalDateTime fechaInicio,
        @NotNull @Future LocalDateTime fechaFin,
        @NotNull TipoEvento tipo,
        @NotNull UUID recintoId
) {
}
