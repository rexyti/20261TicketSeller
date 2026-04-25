package com.ticketseller.application;

import com.ticketseller.domain.exception.TransicionEstadoInvalidaException;
import com.ticketseller.domain.model.EstadoTicket;
import com.ticketseller.domain.model.HistorialTicket;
import com.ticketseller.domain.model.Ticket;
import com.ticketseller.domain.repository.HistorialTicketRepositoryPort;
import com.ticketseller.domain.repository.NotificacionEmailPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class CambiarEstadoTicketUseCase {
    private final TicketRepositoryPort ticketRepositoryPort;
    private final HistorialTicketRepositoryPort historialRepositoryPort;
    private final NotificacionEmailPort notificacionEmailPort;
    private final VentaRepositoryPort ventaRepositoryPort;

    public Mono<Ticket> ejecutar(UUID ticketId, EstadoTicket nuevoEstado, String justificacion, UUID agenteId) {
        return ticketRepositoryPort.buscarPorId(ticketId)
                .flatMap(ticket -> {
                    validarTransicion(ticket.getEstado(), nuevoEstado);
                    
                    EstadoTicket estadoAnterior = ticket.getEstado();
                    Ticket ticketActualizado = ticket.toBuilder().estado(nuevoEstado).build();
                    
                    return ticketRepositoryPort.guardar(ticketActualizado)
                            .flatMap(guardado -> registrarHistorial(guardado, estadoAnterior, justificacion, agenteId))
                            .flatMap(guardado -> notificarSiEsAnulado(guardado, justificacion));
                });
    }

    private void validarTransicion(EstadoTicket origen, EstadoTicket destino) {
        if (origen.equals(destino)) return;
        
        boolean permitida = switch (origen) {
            case VENDIDO -> true;
            case CANCELADO -> destino == EstadoTicket.REEMBOLSO_PENDIENTE || destino == EstadoTicket.REEMBOLSADO;
            case REEMBOLSO_PENDIENTE -> destino == EstadoTicket.REEMBOLSADO;
            default -> false;
        };

        if (!permitida) {
            throw new TransicionEstadoInvalidaException(origen, destino);
        }
    }

    private Mono<Ticket> registrarHistorial(Ticket ticket, EstadoTicket anterior, String justificacion, UUID agenteId) {
        HistorialTicket historial = HistorialTicket.builder()
                .id(UUID.randomUUID())
                .ticketId(ticket.getId())
                .agenteId(agenteId)
                .estadoAnterior(anterior)
                .estadoNuevo(ticket.getEstado())
                .fecha(LocalDateTime.now())
                .justificacion(justificacion)
                .build();
        
        return historialRepositoryPort.save(historial).thenReturn(ticket);
    }

    private Mono<Ticket> notificarSiEsAnulado(Ticket ticket, String motivo) {
        if (EstadoTicket.ANULADO.equals(ticket.getEstado())) {
            return ventaRepositoryPort.buscarPorId(ticket.getVentaId())
                    .flatMap(venta -> notificacionEmailPort.enviarAnulacion(venta, ticket, motivo))
                    .thenReturn(ticket);
        }
        return Mono.just(ticket);
    }
}
