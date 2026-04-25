package com.ticketseller.application.postventa;

import com.ticketseller.domain.model.EstadoTicket;
import com.ticketseller.domain.repository.ReembolsoRepositoryPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import com.ticketseller.infrastructure.adapter.in.rest.dto.TicketConReembolsoResponse;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
public class ConsultarEstadoReembolsoUseCase {
    private final VentaRepositoryPort ventaRepositoryPort;
    private final TicketRepositoryPort ticketRepositoryPort;
    private final ReembolsoRepositoryPort reembolsoRepositoryPort;

    public Flux<TicketConReembolsoResponse> ejecutar(UUID compradorId) {
        Set<EstadoTicket> estados = Set.of(EstadoTicket.CANCELADO, EstadoTicket.REEMBOLSO_PENDIENTE, EstadoTicket.REEMBOLSADO);
        
        return ventaRepositoryPort.buscarPorComprador(compradorId)
                .flatMap(venta -> ticketRepositoryPort.buscarPorVenta(venta.getId()))
                .filter(ticket -> estados.contains(ticket.getEstado()))
                .flatMap(ticket -> reembolsoRepositoryPort.findByTicketId(ticket.getId())
                        .map(reembolso -> new TicketConReembolsoResponse(
                                ticket.getId(),
                                ticket.getVentaId(),
                                ticket.getEventoId(),
                                ticket.getEstado(),
                                ticket.getPrecio(),
                                ticket.getCodigoQr(),
                                reembolso.getEstado(),
                                reembolso.getMonto(),
                                reembolso.getFechaSolicitud()
                        ))
                        .defaultIfEmpty(new TicketConReembolsoResponse(
                                ticket.getId(),
                                ticket.getVentaId(),
                                ticket.getEventoId(),
                                ticket.getEstado(),
                                ticket.getPrecio(),
                                ticket.getCodigoQr(),
                                null,
                                null,
                                null
                        )));
    }
}
