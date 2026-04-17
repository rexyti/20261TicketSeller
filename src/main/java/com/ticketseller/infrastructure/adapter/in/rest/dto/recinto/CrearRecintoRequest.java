package com.ticketseller.infrastructure.adapter.in.rest.dto.recinto;

import jakarta.validation.constraints.*;

public record CrearRecintoRequest(
        @NotBlank String nombre,
        @NotBlank String ciudad,
        @NotBlank String direccion,
        @NotNull @Positive Integer capacidadMaxima,
        @NotBlank String telefono,
        @NotNull @Positive Integer compuertasIngreso
) {
}

