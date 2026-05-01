package com.ticketseller.infrastructure.adapter.in.rest.checkout.dto;

import com.ticketseller.domain.model.ticket.EstadoTicket;

import java.math.BigDecimal;
import java.util.UUID;

public record TicketResponse(
        UUID id,
        UUID zonaId,
        UUID compuertaId,
        EstadoTicket estado,
        BigDecimal precio,
        String codigoQr,
        boolean esCortesia
) {
}
