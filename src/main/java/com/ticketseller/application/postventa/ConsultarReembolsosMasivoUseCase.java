package com.ticketseller.application.postventa;

import com.ticketseller.domain.model.postventa.Reembolso;
import com.ticketseller.domain.model.ticket.EstadoTicket;
import com.ticketseller.domain.repository.ReembolsoRepositoryPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
public class ConsultarReembolsosMasivoUseCase {

    private final TicketRepositoryPort ticketRepositoryPort;
    private final ReembolsoRepositoryPort reembolsoRepositoryPort;

    public Flux<Reembolso> ejecutar(UUID eventoId, int page, int size) {
        return ticketRepositoryPort.buscarPorEventoYEstados(eventoId, Set.of(EstadoTicket.CANCELADO))
                .flatMap(ticket -> reembolsoRepositoryPort.buscarPorTicketId(ticket.getId()))
                .skip((long) page * size)
                .take(size);
    }
}
