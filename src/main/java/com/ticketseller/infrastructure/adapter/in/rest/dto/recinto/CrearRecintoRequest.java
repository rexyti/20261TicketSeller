package com.ticketseller.infrastructure.adapter.in.rest.dto.recinto;

import jakarta.validation.constraints.*;

public record CrearRecintoRequest(
        @NotBlank @Size(max = 120) String nombre,
        @NotBlank @Size(max = 80) String ciudad,
        @NotBlank @Size(max = 200) String direccion,
        @NotNull @Positive Integer capacidadMaxima,
        @NotBlank @Size(max = 20) @Pattern(regexp = "^[0-9+\\-() ]+$") String telefono,
        @NotNull @Positive Integer compuertasIngreso
) {
}

