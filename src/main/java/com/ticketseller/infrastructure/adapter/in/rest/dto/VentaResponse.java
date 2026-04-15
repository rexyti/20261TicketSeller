package com.ticketseller.infrastructure.adapter.in.rest.dto;

import com.ticketseller.domain.model.EstadoVenta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record VentaResponse(
        UUID id,
        UUID compradorId,
        UUID eventoId,
        EstadoVenta estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaExpiracion,
        BigDecimal total
) {}
