package com.ticketseller.infrastructure.adapter.in.rest.evento.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record CrearEventoRequest(
        @NotBlank String nombre,
        @NotNull @Future LocalDateTime fechaInicio,
        @NotNull @Future LocalDateTime fechaFin,
        @NotBlank String tipo,
        @NotNull UUID recintoId
) {
}

