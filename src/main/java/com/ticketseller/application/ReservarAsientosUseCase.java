package com.ticketseller.application;

import com.ticketseller.domain.exception.AsientoNoDisponibleException;
import com.ticketseller.domain.model.EstadoTicket;
import com.ticketseller.domain.model.EstadoVenta;
import com.ticketseller.domain.model.Ticket;
import com.ticketseller.domain.model.Venta;
import com.ticketseller.domain.port.out.TicketRepositoryPort;
import com.ticketseller.domain.port.out.VentaRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservarAsientosUseCase {

    private static final int RESERVA_MINUTOS = 15;

    private final TicketRepositoryPort ticketRepository;
    private final VentaRepositoryPort ventaRepository;

    public Mono<Venta> ejecutar(UUID eventoId, UUID compradorId, List<UUID> ticketIds, BigDecimal total) {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime fechaExpiracion = ahora.plusMinutes(RESERVA_MINUTOS);

        return verificarDisponibilidad(ticketIds)
                .then(crearVentaReservada(eventoId, compradorId, fechaExpiracion, total))
                .flatMap(venta -> reservarTickets(ticketIds, venta.getId())
                        .thenReturn(venta));
    }

    private Mono<Void> verificarDisponibilidad(List<UUID> ticketIds) {
        return Flux.fromIterable(ticketIds)
                .flatMap(ticketRepository::buscarPorId)
                .flatMap(ticket -> {
                    if (ticket.getEstado() != EstadoTicket.DISPONIBLE) {
                        return Mono.<Void>error(new AsientoNoDisponibleException(
                                "El ticket " + ticket.getId() + " no está disponible, estado: " + ticket.getEstado()));
                    }
                    return Mono.empty();
                })
                .then();
    }

    private Mono<Venta> crearVentaReservada(UUID eventoId, UUID compradorId, LocalDateTime fechaExpiracion, BigDecimal total) {
        Venta venta = new Venta(
                UUID.randomUUID(),
                compradorId,
                eventoId,
                EstadoVenta.RESERVADA,
                LocalDateTime.now(),
                fechaExpiracion,
                total
        );
        return ventaRepository.guardar(venta);
    }

    private Mono<Void> reservarTickets(List<UUID> ticketIds, UUID ventaId) {
        return Flux.fromIterable(ticketIds)
                .flatMap(ticketId -> ticketRepository.buscarPorId(ticketId)
                        .flatMap(ticket -> {
                            Ticket ticketReservado = ticket.withEstado(EstadoTicket.RESERVADO);
                            Ticket ticketConVenta = new Ticket(
                                    ticketReservado.getId(),
                                    ventaId,
                                    ticketReservado.getEventoId(),
                                    ticketReservado.getZonaId(),
                                    ticketReservado.getCompuertaId(),
                                    ticketReservado.getCodigoQR(),
                                    ticketReservado.getEstado(),
                                    ticketReservado.getPrecio(),
                                    ticketReservado.isEsCortesia()
                            );
                            return ticketRepository.guardar(ticketConVenta);
                        }))
                .then();
    }
}
