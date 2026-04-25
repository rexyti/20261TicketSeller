package com.ticketseller.infrastructure.adapter.in.rest.dto.tipoasiento;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AsignarTipoAsientoRequest(
        @NotNull UUID tipoAsientoId
) {
}
