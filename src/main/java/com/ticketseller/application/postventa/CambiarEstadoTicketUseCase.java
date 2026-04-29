package com.ticketseller.application.postventa;

import com.ticketseller.domain.exception.postventa.TransicionEstadoInvalidaException;
import com.ticketseller.domain.exception.venta.TicketNotFoundException;
import com.ticketseller.domain.model.postventa.HistorialEstadoTicket;
import com.ticketseller.domain.model.ticket.EstadoTicket;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.domain.repository.HistorialEstadoTicketRepositoryPort;
import com.ticketseller.domain.repository.NotificacionEmailPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
public class CambiarEstadoTicketUseCase {
    private final TicketRepositoryPort ticketRepositoryPort;
    private final HistorialEstadoTicketRepositoryPort historialEstadoTicketRepositoryPort;
    private final NotificacionEmailPort notificacionEmailPort;

    public Mono<Ticket> ejecutar(UUID ticketId, EstadoTicket nuevoEstado, String justificacion, UUID agenteId) {
        if (nuevoEstado == null) {
            return Mono.error(new IllegalArgumentException("estado es obligatorio"));
        }
        if (justificacion == null || justificacion.isBlank()) {
            return Mono.error(new IllegalArgumentException("justificacion es obligatoria"));
        }
        return ticketRepositoryPort.buscarPorId(ticketId)
                .switchIfEmpty(Mono.error(new TicketNotFoundException("Ticket no encontrado: " + ticketId)))
                .flatMap(ticket -> validarTransicion(ticket.getEstado(), nuevoEstado).thenReturn(ticket))
                .flatMap(ticket -> persistirCambio(ticket, nuevoEstado, justificacion.trim(), agenteId));
    }

    private Mono<Void> validarTransicion(EstadoTicket actual, EstadoTicket destino) {
        if (actual == destino) {
            return Mono.error(new TransicionEstadoInvalidaException(actual, destino));
        }
        if (!estadoPermitido(actual).contains(destino)) {
            return Mono.error(new TransicionEstadoInvalidaException(actual, destino));
        }
        return Mono.empty();
    }

    private Set<EstadoTicket> estadoPermitido(EstadoTicket actual) {
        return switch (actual) {
            case VENDIDO -> Set.of(EstadoTicket.CANCELADO, EstadoTicket.ANULADO, EstadoTicket.REEMBOLSO_PENDIENTE);
            case CANCELADO -> Set.of(EstadoTicket.REEMBOLSO_PENDIENTE, EstadoTicket.REEMBOLSADO, EstadoTicket.ANULADO, EstadoTicket.VENDIDO);
            case REEMBOLSO_PENDIENTE -> Set.of(EstadoTicket.REEMBOLSADO, EstadoTicket.ANULADO);
            case ANULADO -> Set.of(EstadoTicket.VENDIDO);
            default -> Set.of();
        };
    }

    private Mono<Ticket> persistirCambio(Ticket ticket, EstadoTicket nuevoEstado, String justificacion, UUID agenteId) {
        Ticket actualizado = ticket.toBuilder().estado(nuevoEstado).build();
        HistorialEstadoTicket historial = HistorialEstadoTicket.builder()
                .id(UUID.randomUUID())
                .ticketId(ticket.getId())
                .agenteId(agenteId)
                .estadoAnterior(ticket.getEstado())
                .estadoNuevo(nuevoEstado)
                .justificacion(justificacion)
                .fechaCambio(LocalDateTime.now())
                .build();
        return ticketRepositoryPort.guardar(actualizado)
                .flatMap(saved -> historialEstadoTicketRepositoryPort.guardar(historial)
                        .then(notificarSiAnulado(saved, justificacion))
                        .thenReturn(saved));
    }

    private Mono<Void> notificarSiAnulado(Ticket ticket, String justificacion) {
        if (!EstadoTicket.ANULADO.equals(ticket.getEstado())) {
            return Mono.empty();
        }
        return notificacionEmailPort.enviarCancelacionTicket(ticket, justificacion);
    }
}

