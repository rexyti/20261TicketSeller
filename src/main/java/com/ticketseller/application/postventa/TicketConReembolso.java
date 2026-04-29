package com.ticketseller.application.postventa;

import com.ticketseller.domain.model.postventa.Reembolso;
import com.ticketseller.domain.model.ticket.Ticket;

public record TicketConReembolso(
        Ticket ticket,
        Reembolso reembolso
) {
}

