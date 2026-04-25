package com.ticketseller.infrastructure.adapter.in.rest.dto.tipoasiento;

import jakarta.validation.constraints.NotBlank;

public record CrearTipoAsientoRequest(
        @NotBlank String nombre,
        String descripcion
) {
}
