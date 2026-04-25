package com.ticketseller.application.postventa;

import com.ticketseller.domain.exception.TicketNotFoundException;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import com.ticketseller.infrastructure.adapter.in.rest.dto.TicketEstadoResponse;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class ConsultarEstadoTicketUseCase {

    private final TicketRepositoryPort ticketRepositoryPort;

    public Mono<TicketEstadoResponse> ejecutar(UUID ticketId) {
        return ticketRepositoryPort.findById(ticketId)
                .switchIfEmpty(Mono.error(new TicketNotFoundException("Ticket no encontrado: " + ticketId)))
                .flatMap(ticket -> {
                    // TODO: aplicar estrategia de expiración de TTL — NEEDS CLARIFICATION (FR-003)
                    
                    return Mono.just(new TicketEstadoResponse(
                            ticket.getId(),
                            ticket.getEstado(),
                            ticket.getCategoria(),
                            ticket.getBloque(),
                            ticket.getCoordenadaAcceso(),
                            ticket.getEventoId(),
                            ticket.getFechaEvento()
                    ));
                });
    }
}
