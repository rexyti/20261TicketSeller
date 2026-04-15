package com.ticketseller.application;

import com.ticketseller.domain.model.EstadoTicket;
import com.ticketseller.domain.model.EstadoVenta;
import com.ticketseller.domain.model.Ticket;
import com.ticketseller.domain.model.Venta;
import com.ticketseller.domain.port.out.TicketRepositoryPort;
import com.ticketseller.domain.port.out.VentaRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiberarReservaUseCase {

    private final VentaRepositoryPort ventaRepository;
    private final TicketRepositoryPort ticketRepository;

    public Mono<Void> ejecutar() {
        LocalDateTime ahora = LocalDateTime.now();
        return ventaRepository.buscarVentasExpiradas(ahora)
                .filter(venta -> venta.getEstado() == EstadoVenta.RESERVADA)
                .flatMap(this::liberarVenta)
                .then();
    }

    private Mono<Void> liberarVenta(Venta venta) {
        log.info("Liberando reserva expirada para venta {}", venta.getId());
        return ventaRepository.actualizarEstado(venta.getId(), EstadoVenta.EXPIRADA)
                .then(liberarTickets(venta.getId()));
    }

    private Mono<Void> liberarTickets(UUID ventaId) {
        return ticketRepository.buscarPorVenta(ventaId)
                .filter(ticket -> ticket.getEstado() == EstadoTicket.RESERVADO)
                .flatMap(ticket -> ticketRepository.actualizarEstado(ticket.getId(), EstadoTicket.DISPONIBLE))
                .then();
    }
}
