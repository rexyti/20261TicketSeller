package com.ticketseller.application.checkout;

import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.domain.model.venta.Venta;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

import java.util.UUID;

@RequiredArgsConstructor
public class ConsultarTodosTicketsUseCase {
    private final VentaRepositoryPort ventaRepositoryPort;
    private final TicketRepositoryPort ticketRepositoryPort;

    public Flux<Ticket> ejecutar(UUID compradorId) {
        return ventaRepositoryPort.buscarPorCompradorId(compradorId)
                .map(Venta::getId)
                .collectList()
                .flatMapMany(ticketRepositoryPort::buscarPorVentaIds);
    }
}
