package com.ticketseller.application.postventa;

import com.ticketseller.domain.model.ticket.EstadoTicket;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.domain.model.venta.Venta;
import com.ticketseller.domain.repository.ReembolsoRepositoryPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
public class ConsultarEstadoReembolsoUseCase {
    private final VentaRepositoryPort ventaRepositoryPort;
    private final TicketRepositoryPort ticketRepositoryPort;
    private final ReembolsoRepositoryPort reembolsoRepositoryPort;

    public Flux<TicketConReembolso> ejecutar(UUID compradorId) {
        return ventaRepositoryPort.buscarPorCompradorId(compradorId)
                .map(Venta::getId)
                .collectList()
                .flatMapMany(ticketRepositoryPort::buscarPorVentaIds)
                .filter(this::isTicketCanceladoOReembolsado)
                .flatMap(ticket -> reembolsoRepositoryPort.buscarPorTicketId(ticket.getId())
                        .map(reembolso -> new TicketConReembolso(ticket, reembolso))
                        .switchIfEmpty(Mono.just(new TicketConReembolso(ticket, null))));
    }

    private boolean isTicketCanceladoOReembolsado(Ticket ticket){
        var estadoActual = ticket.getEstado();
        return EstadoTicket.CANCELADO.equals(estadoActual) || EstadoTicket.REEMBOLSADO.equals(estadoActual)
                || EstadoTicket.REEMBOLSO_PENDIENTE.equals(estadoActual);
    }
}

