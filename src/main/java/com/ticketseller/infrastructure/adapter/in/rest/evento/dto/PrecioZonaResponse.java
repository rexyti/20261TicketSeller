package com.ticketseller.infrastructure.adapter.in.rest.evento.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PrecioZonaResponse(
        UUID id,
        UUID eventoId,
        UUID zonaId,
        BigDecimal precio
) {
}

