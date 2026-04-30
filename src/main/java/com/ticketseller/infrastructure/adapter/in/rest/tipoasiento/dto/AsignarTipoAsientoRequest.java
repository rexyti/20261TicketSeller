package com.ticketseller.infrastructure.adapter.in.rest.tipoasiento.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AsignarTipoAsientoRequest(
        @NotNull UUID tipoAsientoId
) {
}
