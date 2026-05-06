package com.ticketseller.infrastructure.adapter.in.rest.asiento.dto;

import java.util.UUID;

public record DisponibilidadResponse(
        UUID asientoId,
        boolean disponible,
        String estado,
        String mensaje
) {
}
