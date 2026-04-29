package com.ticketseller.application.postventa;

import com.ticketseller.domain.model.postventa.EstadoReembolso;
import com.ticketseller.domain.model.postventa.Reembolso;
import com.ticketseller.domain.model.postventa.TipoReembolso;
import com.ticketseller.domain.model.ticket.EstadoTicket;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.domain.repository.ReembolsoRepositoryPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
public class ProcesarReembolsoMasivoUseCase {
    private final TicketRepositoryPort ticketRepositoryPort;
    private final ReembolsoRepositoryPort reembolsoRepositoryPort;

    public Mono<Void> ejecutar(UUID eventoId) {
        return ticketRepositoryPort.buscarPorEventoYEstados(eventoId, Set.of(EstadoTicket.VENDIDO))
                .flatMap(this::cancelarYCrearReembolso)
                .then();
    }

    private Mono<Void> cancelarYCrearReembolso(Ticket ticket) {
        EstadoTicket nuevoEstado = ticket.isEsCortesia() ? EstadoTicket.ANULADO : EstadoTicket.CANCELADO;
        return ticketRepositoryPort.guardar(ticket.toBuilder().estado(nuevoEstado).build())
                .flatMap(saved -> saved.isEsCortesia() ? Mono.empty() : crearReembolsoPendiente(saved).then());
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
}

