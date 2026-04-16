package com.ticketseller.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CrearRecintoRequest(
        @NotBlank String nombre,
        @NotBlank String ciudad,
        @NotBlank String direccion,
        @NotNull @Min(1) Integer capacidadMaxima,
        @NotBlank String telefono,
        @NotNull @Min(0) Integer compuertasIngreso
) {
}

