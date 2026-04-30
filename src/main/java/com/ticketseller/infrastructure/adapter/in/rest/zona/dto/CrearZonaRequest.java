package com.ticketseller.infrastructure.adapter.in.rest.zona.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CrearZonaRequest(
        @NotBlank String nombre,
        @NotNull @Min(1) Integer capacidad
) {
}

