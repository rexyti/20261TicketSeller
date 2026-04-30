package com.ticketseller.infrastructure.adapter.in.rest.compuerta.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CrearCompuertaRequest(
        @NotBlank String nombre,
        UUID zonaId
) {
}

