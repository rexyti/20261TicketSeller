package com.ticketseller.application.postventa;

import com.ticketseller.domain.exception.postventa.CancelacionFueraDePlazoException;
import com.ticketseller.domain.exception.postventa.TicketYaUsadoException;
import com.ticketseller.domain.exception.venta.TicketNotFoundException;
import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import com.ticketseller.domain.model.evento.Evento;
import com.ticketseller.domain.model.postventa.EstadoReembolso;
import com.ticketseller.domain.model.postventa.Reembolso;
import com.ticketseller.domain.model.postventa.TipoReembolso;
import com.ticketseller.domain.model.ticket.EstadoTicket;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import com.ticketseller.domain.repository.ReembolsoRepositoryPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class CancelarTicketUseCase {
    private final TicketRepositoryPort ticketRepositoryPort;
    private final AsientoRepositoryPort asientoRepositoryPort;
    private final EventoRepositoryPort eventoRepositoryPort;
    private final ReembolsoRepositoryPort reembolsoRepositoryPort;

    public Mono<CancelacionResultado> cancelarTicket(UUID ticketId) {
        return cancelarVarios(List.of(ticketId));
    }

    public Mono<CancelacionResultado> cancelarVarios(List<UUID> ticketIds) {
        if (ticketIds == null || ticketIds.isEmpty()) {
            return Mono.error(new IllegalArgumentException("ticketIds es obligatorio"));
        }
        return Flux.fromIterable(ticketIds)
                .distinct()
                .flatMap(this::cancelarYEncolarReembolso)
                .collectList()
                .map(this::armarResultado);
    }

    private Mono<CancelacionTicket> cancelarYEncolarReembolso(UUID ticketId) {
        return ticketRepositoryPort.buscarPorId(ticketId)
                .switchIfEmpty(Mono.error(new TicketNotFoundException("Ticket no encontrado: " + ticketId)))
                .flatMap(this::validarTicketCancelable)
                .flatMap(this::cancelarTicketYLiberarAsiento)
                .flatMap(ticket -> crearReembolsoPendiente(ticket).map(reembolso -> new CancelacionTicket(ticket, reembolso)));
    }

    private Mono<Ticket> validarTicketCancelable(Ticket ticket) {
        if (EstadoTicket.USADO.equals(ticket.getEstado())) {
            return Mono.error(new TicketYaUsadoException("No se puede cancelar un ticket ya usado"));
        }
        return eventoRepositoryPort.buscarPorId(ticket.getEventoId())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Evento no encontrado para ticket")))
                .flatMap(evento -> validarPlazo(evento).thenReturn(ticket));
    }

    private Mono<Void> validarPlazo(Evento evento) {
        if (evento.getFechaInicio() != null && !LocalDateTime.now().isBefore(evento.getFechaInicio())) {
            return Mono.error(new CancelacionFueraDePlazoException("No se puede cancelar: el evento ya ocurrió"));
        }
        return Mono.empty();
    }

    private Mono<Ticket> cancelarTicketYLiberarAsiento(Ticket ticket) {
        Ticket cancelado = ticket.toBuilder().estado(EstadoTicket.CANCELADO).build();
        return ticketRepositoryPort.guardar(cancelado)
                .flatMap(saved -> liberarAsiento(saved).thenReturn(saved));
    }

    private Mono<Void> liberarAsiento(Ticket ticket) {
        if (ticket.getAsientoId() == null) {
            return Mono.empty();
        }
        return asientoRepositoryPort.buscarPorId(ticket.getAsientoId())
                .flatMap(this::guardarAsientoDisponible)
                .then();
    }

    private Mono<Asiento> guardarAsientoDisponible(Asiento asiento) {
        return asientoRepositoryPort.guardar(asiento.toBuilder().estado(EstadoAsiento.DISPONIBLE).build());
    }

    private Mono<Reembolso> crearReembolsoPendiente(Ticket ticket) {
        Reembolso reembolso = Reembolso.builder()
                .id(UUID.randomUUID())
                .ticketId(ticket.getId())
                .ventaId(ticket.getVentaId())
                .monto(ticket.getPrecio())
                .tipo(TipoReembolso.TOTAL)
                .estado(EstadoReembolso.PENDIENTE)
                .fechaSolicitud(LocalDateTime.now())
                .build();
        reembolso.validarDatosRegistro();
        return reembolsoRepositoryPort.guardar(reembolso);
    }

    private CancelacionResultado armarResultado(List<CancelacionTicket> cancelaciones) {
        List<UUID> ticketsCancelados = cancelaciones.stream().map(c -> c.ticket().getId()).toList();
        BigDecimal montoTotal = cancelaciones.stream()
                .map(c -> c.reembolso().getMonto())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        UUID primerReembolsoId = cancelaciones.stream()
                .map(c -> c.reembolso().getId())
                .min(Comparator.comparing(UUID::toString))
                .orElse(null);
        return new CancelacionResultado(
                ticketsCancelados,
                primerReembolsoId,
                montoTotal,
                cancelaciones.stream().map(CancelacionTicket::reembolso).toList()
        );
    }

    private record CancelacionTicket(Ticket ticket, Reembolso reembolso) {
    }
}

