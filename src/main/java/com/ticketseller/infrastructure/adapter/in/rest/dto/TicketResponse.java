package com.ticketseller.infrastructure.adapter.in.rest.dto;

import com.ticketseller.domain.model.EstadoTicket;

import java.math.BigDecimal;
import java.util.UUID;

public record TicketResponse(
        UUID id,
        UUID ventaId,
        UUID eventoId,
        UUID zonaId,
        UUID compuertaId,
        String codigoQR,
        EstadoTicket estado,
        BigDecimal precio,
        boolean esCortesia
) {}
