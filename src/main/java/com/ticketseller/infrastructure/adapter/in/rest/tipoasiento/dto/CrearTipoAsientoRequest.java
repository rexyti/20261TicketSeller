package com.ticketseller.infrastructure.adapter.in.rest.tipoasiento.dto;

import jakarta.validation.constraints.NotBlank;

public record CrearTipoAsientoRequest(
        @NotBlank String nombre,
        String descripcion
) {
}
