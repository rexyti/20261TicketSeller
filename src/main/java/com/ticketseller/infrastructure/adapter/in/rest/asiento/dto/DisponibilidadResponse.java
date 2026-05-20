package com.ticketseller.infrastructure.adapter.in.rest.asiento.dto;

import java.util.UUID;

public record DisponibilidadResponse(
        UUID asientoId,
        String numeroAsiento,
        UUID zonaId,
        String tipoAsiento,
        String estado
) {
}
