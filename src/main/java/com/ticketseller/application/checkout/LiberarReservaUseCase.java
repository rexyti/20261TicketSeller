package com.ticketseller.application.checkout;

import com.ticketseller.domain.model.EstadoVenta;
import com.ticketseller.domain.model.Venta;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class LiberarReservaUseCase {

    private final VentaRepositoryPort ventaRepositoryPort;
    private final TicketRepositoryPort ticketRepositoryPort;

    public Mono<Void> ejecutar() {
        return ventaRepositoryPort.buscarVentasExpiradas(LocalDateTime.now())
                .flatMap(this::liberarVenta)
                .then();
    }

    private Mono<Venta> liberarVenta(Venta venta) {
        return ventaRepositoryPort.actualizarEstado(venta.getId(), EstadoVenta.EXPIRADA);
    }
}
