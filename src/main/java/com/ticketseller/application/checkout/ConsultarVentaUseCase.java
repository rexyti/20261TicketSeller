package com.ticketseller.application.checkout;

import com.ticketseller.domain.exception.VentaNotFoundException;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class ConsultarVentaUseCase {

    private final VentaRepositoryPort ventaRepositoryPort;
    private final TicketRepositoryPort ticketRepositoryPort;

    public Mono<VentaDetalle> ejecutar(UUID ventaId) {
        return ventaRepositoryPort.buscarPorId(ventaId)
                .switchIfEmpty(Mono.error(new VentaNotFoundException("Venta no encontrada")))
                .flatMap(venta -> ticketRepositoryPort.buscarPorVenta(ventaId)
                        .collectList()
                        .map(tickets -> new VentaDetalle(venta, tickets)));
    }
}

