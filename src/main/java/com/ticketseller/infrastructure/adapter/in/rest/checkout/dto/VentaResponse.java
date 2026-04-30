package com.ticketseller.infrastructure.adapter.in.rest.checkout.dto;

import com.ticketseller.domain.model.venta.EstadoVenta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record VentaResponse(
        UUID id,
        UUID compradorId,
        UUID eventoId,
        EstadoVenta estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaExpiracion,
        BigDecimal total,
        List<TicketResponse> tickets
) {
}

