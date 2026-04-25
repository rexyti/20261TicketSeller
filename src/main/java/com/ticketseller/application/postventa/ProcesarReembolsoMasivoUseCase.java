package com.ticketseller.application.postventa;

import com.ticketseller.domain.model.*;
import com.ticketseller.domain.repository.ReembolsoRepositoryPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class ProcesarReembolsoMasivoUseCase {
    private final TicketRepositoryPort ticketRepositoryPort;
    private final ReembolsoRepositoryPort reembolsoRepositoryPort;

    public Mono<Void> ejecutar(UUID eventoId) {
        return ticketRepositoryPort.buscarPorEvento(eventoId)
                .filter(ticket -> EstadoTicket.VENDIDO.equals(ticket.getEstado()))
                .flatMap(this::cancelarYCrearReembolso)
                .then();
    }

    private Mono<Void> cancelarYCrearReembolso(Ticket ticket) {
        if (ticket.isEsCortesia()) {
            return ticketRepositoryPort.guardar(ticket.toBuilder().estado(EstadoTicket.ANULADO).build())
                    .then();
        }

        Ticket ticketCancelado = ticket.toBuilder().estado(EstadoTicket.CANCELADO).build();
        
        return ticketRepositoryPort.guardar(ticketCancelado)
                .flatMap(t -> {
                    Reembolso reembolso = Reembolso.builder()
                            .id(UUID.randomUUID())
                            .ticketId(t.getId())
                            .ventaId(t.getVentaId())
                            .monto(t.getPrecio())
                            .tipo(TipoReembolso.TOTAL)
                            .estado(EstadoReembolso.PENDIENTE)
                            .fechaSolicitud(LocalDateTime.now())
                            .build();
                    return reembolsoRepositoryPort.save(reembolso);
                })
                .then();
    }
}
