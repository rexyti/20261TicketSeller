package com.ticketseller.application;

import com.ticketseller.domain.exception.VentaNotFoundException;
import com.ticketseller.domain.model.Ticket;
import com.ticketseller.domain.model.Venta;
import com.ticketseller.domain.port.out.TicketRepositoryPort;
import com.ticketseller.domain.port.out.VentaRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConsultarVentaUseCase {

    private final VentaRepositoryPort ventaRepository;
    private final TicketRepositoryPort ticketRepository;

    public record VentaDetalle(Venta venta, List<Ticket> tickets) {}

    public Mono<VentaDetalle> ejecutar(UUID ventaId) {
        return ventaRepository.buscarPorId(ventaId)
                .switchIfEmpty(Mono.error(new VentaNotFoundException(ventaId)))
                .flatMap(venta -> ticketRepository.buscarPorVenta(ventaId)
                        .collectList()
                        .map(tickets -> new VentaDetalle(venta, tickets)));
    }
}
