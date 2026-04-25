package com.ticketseller.application.postventa;

import com.ticketseller.domain.exception.CancelacionFueraDePlazoException;
import com.ticketseller.domain.exception.TicketYaUsadoException;
import com.ticketseller.domain.model.*;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import com.ticketseller.domain.repository.ReembolsoRepositoryPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import com.ticketseller.infrastructure.adapter.in.rest.dto.CancelacionResponse;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class CancelarTicketUseCase {
    private final TicketRepositoryPort ticketRepositoryPort;
    private final AsientoRepositoryPort asientoRepositoryPort;
    private final ReembolsoRepositoryPort reembolsoRepositoryPort;
    private final EventoRepositoryPort eventoRepositoryPort;

    public Mono<CancelacionResponse> ejecutar(List<UUID> ticketIds) {
        return Flux.fromIterable(ticketIds)
                .flatMap(this::procesarCancelacionIndividual)
                .collectList()
                .flatMap(this::crearReembolsoParaTicketsCancelados);
    }

    private Mono<Ticket> procesarCancelacionIndividual(UUID ticketId) {
        return ticketRepositoryPort.buscarPorId(ticketId)
                .flatMap(this::validarTicketYEvento)
                .flatMap(this::actualizarTicketYAsiento);
    }

    private Mono<Ticket> validarTicketYEvento(Ticket ticket) {
        if (EstadoTicket.USADO.equals(ticket.getEstado())) {
            return Mono.error(new TicketYaUsadoException(ticket.getId()));
        }

        return eventoRepositoryPort.buscarPorId(ticket.getEventoId())
                .flatMap(evento -> {
                    if (evento.getFechaInicio().isBefore(LocalDateTime.now())) {
                        return Mono.error(new CancelacionFueraDePlazoException("No se puede cancelar un ticket de un evento que ya ocurrió o está en curso."));
                    }
                    return Mono.just(ticket);
                });
    }

    private Mono<Ticket> actualizarTicketYAsiento(Ticket ticket) {
        Ticket ticketCancelado = ticket.toBuilder().estado(EstadoTicket.CANCELADO).build();
        
        return ticketRepositoryPort.guardar(ticketCancelado)
                .flatMap(t -> {
                    if (t.getAsientoId() != null) {
                        return asientoRepositoryPort.buscarPorId(t.getAsientoId())
                                .flatMap(asiento -> {
                                    Asiento asientoLiberado = asiento.toBuilder().estado(EstadoAsiento.DISPONIBLE).build();
                                    return asientoRepositoryPort.guardar(asientoLiberado);
                                })
                                .thenReturn(t);
                    }
                    return Mono.just(t);
                });
    }

    private Mono<CancelacionResponse> crearReembolsoParaTicketsCancelados(List<Ticket> tickets) {
        if (tickets.isEmpty()) {
            return Mono.empty();
        }

        BigDecimal montoTotal = tickets.stream()
                .map(Ticket::getPrecio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        UUID ventaId = tickets.get(0).getVentaId();
        
        Reembolso reembolso = Reembolso.builder()
                .id(UUID.randomUUID())
                .ventaId(ventaId)
                .ticketId(tickets.size() == 1 ? tickets.get(0).getId() : null)
                .monto(montoTotal)
                .tipo(tickets.size() == 1 ? TipoReembolso.TOTAL : TipoReembolso.PARCIAL)
                .estado(EstadoReembolso.PENDIENTE)
                .fechaSolicitud(LocalDateTime.now())
                .build();

        return reembolsoRepositoryPort.save(reembolso)
                .map(r -> new CancelacionResponse(
                        tickets.stream().map(Ticket::getId).toList(),
                        r.getId(),
                        r.getMonto()
                ));
    }
}
