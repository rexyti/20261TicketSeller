package com.ticketseller.infrastructure.adapter.in.rest.asiento.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DisponibilidadResponse(
        UUID asientoId,
        boolean disponible,
        String estado,
        LocalDateTime expiraEn,
        String mensaje
) {
}
