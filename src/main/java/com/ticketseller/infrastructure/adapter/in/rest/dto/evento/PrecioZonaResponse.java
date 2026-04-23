package com.ticketseller.infrastructure.adapter.in.rest.dto.evento;

import java.math.BigDecimal;
import java.util.UUID;

public record PrecioZonaResponse(
        UUID id,
        UUID eventoId,
        UUID zonaId,
        BigDecimal precio
) {
}

