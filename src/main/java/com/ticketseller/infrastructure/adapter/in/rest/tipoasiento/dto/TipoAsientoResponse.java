package com.ticketseller.infrastructure.adapter.in.rest.tipoasiento.dto;

import java.util.UUID;

public record TipoAsientoResponse(
        UUID id,
        String nombre,
        String descripcion,
        String estado,
        boolean enUso,
        String advertencia
) {
}
