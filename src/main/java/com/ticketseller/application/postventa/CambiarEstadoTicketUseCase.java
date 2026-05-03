package com.ticketseller.application.postventa;

import com.ticketseller.domain.exception.postventa.CambioEstadoTicketSinJustificacionException;
import com.ticketseller.domain.exception.postventa.EstadoTicketInvalidoException;
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
import java.util.UUID;

@RequiredArgsConstructor
public class CambiarEstadoTicketUseCase {
    private final TicketRepositoryPort ticketRepositoryPort;
    private final HistorialEstadoTicketRepositoryPort historialEstadoTicketRepositoryPort;
    private final NotificacionEmailPort notificacionEmailPort;

    public Mono<Ticket> ejecutar(UUID ticketId, EstadoTicket nuevoEstado, String justificacion, UUID agenteId) {
        if (noHayNuevoEstado(nuevoEstado)) {
            return Mono.error(new EstadoTicketInvalidoException("Estado es obligatorio"));
        }
        if (noHayJustificacion(justificacion)) {
            return Mono.error(new CambioEstadoTicketSinJustificacionException("Justificación es obligatoria"));
        }
        String justificacionNormalizada = justificacion.trim();
        return ticketRepositoryPort.buscarPorId(ticketId)
                .switchIfEmpty(Mono.error(new TicketNotFoundException("Ticket no encontrado: " + ticketId)))
                .flatMap(ticket -> {
                    ticket.validarTransicionA(nuevoEstado);
                    return persistirCambio(ticket, nuevoEstado, justificacionNormalizada, agenteId);
                });
    }

    private boolean noHayNuevoEstado(EstadoTicket nuevoEstado){
        return nuevoEstado == null;
    }

    private boolean noHayJustificacion(String justificacion) {
        return justificacion == null || justificacion.isBlank();
    }

    private Mono<Ticket> persistirCambio(Ticket ticket, EstadoTicket nuevoEstado, String justificacion, UUID agenteId) {
        Ticket actualizado = ticket.toBuilder().estado(nuevoEstado).build();
        HistorialEstadoTicket historial = crearHistorial(ticket, nuevoEstado, justificacion, agenteId);
        return ticketRepositoryPort.guardar(actualizado)
                .flatMap(saved -> historialEstadoTicketRepositoryPort.guardar(historial)
                        .then(notificarSiAnulado(saved, justificacion))
                        .thenReturn(saved));
    }

    private HistorialEstadoTicket crearHistorial(Ticket ticket, EstadoTicket nuevoEstado, String justificacion, UUID agenteId) {
        return HistorialEstadoTicket.builder()
                .ticketId(ticket.getId())
                .agenteId(agenteId)
                .estadoAnterior(ticket.getEstado())
                .estadoNuevo(nuevoEstado)
                .justificacion(justificacion)
                .fechaCambio(LocalDateTime.now())
                .build();
    }

    private Mono<Void> notificarSiAnulado(Ticket ticket, String justificacion) {
        if (!EstadoTicket.ANULADO.equals(ticket.getEstado())) {
            return Mono.empty();
        }
        return notificacionEmailPort.enviarCancelacionTicket(ticket, justificacion);
    }
}
