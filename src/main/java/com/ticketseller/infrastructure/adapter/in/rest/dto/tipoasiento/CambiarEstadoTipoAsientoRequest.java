package com.ticketseller.infrastructure.adapter.in.rest.dto.tipoasiento;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoTipoAsientoRequest(
        @NotNull String estado
) {
}
