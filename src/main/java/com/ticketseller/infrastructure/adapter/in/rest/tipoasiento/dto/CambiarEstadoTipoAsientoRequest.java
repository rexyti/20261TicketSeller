package com.ticketseller.infrastructure.adapter.in.rest.tipoasiento.dto;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoTipoAsientoRequest(
        @NotNull String estado
) {
}
