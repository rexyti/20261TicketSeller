package com.ticketseller.application.postventa;

import com.ticketseller.domain.model.ticket.EstadoTicket;
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
                .map(venta -> venta.getId())
                .collectList()
                .flatMapMany(ticketRepositoryPort::buscarPorVentaIds)
                .filter(ticket -> Set.of(
                        EstadoTicket.CANCELADO,
                        EstadoTicket.REEMBOLSO_PENDIENTE,
                        EstadoTicket.REEMBOLSADO
                ).contains(ticket.getEstado()))
                .flatMap(ticket -> reembolsoRepositoryPort.buscarPorTicketId(ticket.getId())
                        .map(reembolso -> new TicketConReembolso(ticket, reembolso))
                        .switchIfEmpty(Mono.just(new TicketConReembolso(ticket, null))));
    }
}

