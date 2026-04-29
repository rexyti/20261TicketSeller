package com.ticketseller.infrastructure.adapter.in.rest.dto.checkout;

import com.ticketseller.domain.model.ticket.EstadoTicket;
import com.ticketseller.domain.model.postventa.EstadoReembolso;

import java.math.BigDecimal;
import java.util.UUID;

public record TicketResponse(
        UUID id,
        UUID zonaId,
        UUID compuertaId,
        EstadoTicket estado,
        BigDecimal precio,
        String codigoQr,
        boolean esCortesia,
        EstadoReembolso estadoReembolso,
        String detalleReembolso
) {
}

