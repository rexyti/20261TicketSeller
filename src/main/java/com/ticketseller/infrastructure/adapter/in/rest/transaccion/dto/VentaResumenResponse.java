package com.ticketseller.infrastructure.adapter.in.rest.transaccion.dto;

import com.ticketseller.domain.model.venta.EstadoVenta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record VentaResumenResponse(
        UUID id,
        UUID compradorId,
        UUID eventoId,
        EstadoVenta estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaExpiracion,
        BigDecimal total
) {
}
