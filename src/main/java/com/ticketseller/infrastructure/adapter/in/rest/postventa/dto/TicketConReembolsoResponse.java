package com.ticketseller.infrastructure.adapter.in.rest.postventa.dto;

import com.ticketseller.domain.model.postventa.EstadoReembolso;
import com.ticketseller.domain.model.ticket.EstadoTicket;

import java.math.BigDecimal;
import java.util.UUID;

public record TicketConReembolsoResponse(
        UUID ticketId,
        EstadoTicket estadoTicket,
        EstadoReembolso estadoReembolso,
        BigDecimal montoReembolso,
        UUID reembolsoId
) {
}

