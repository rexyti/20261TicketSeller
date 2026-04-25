package com.ticketseller.infrastructure.adapter.in.rest.dto.tipoasiento;

public record EditarTipoAsientoRequest(
        String nombre,
        String descripcion
) {
}
