package com.ticketseller.application.checkout;

import com.ticketseller.domain.exception.venta.TicketNotFoundException;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class ConsultarEstadoTicketUseCase {

    private final TicketRepositoryPort ticketRepositoryPort;

    public Mono<Ticket> ejecutar(UUID ticketId) {
        return ticketRepositoryPort.findById(ticketId)
                .switchIfEmpty(Mono.error(new TicketNotFoundException("Ticket no encontrado: " + ticketId)));
    }
}
