package com.ticketseller.domain.port.out;

import com.ticketseller.domain.model.Ticket;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface TicketRepositoryPort {
    Mono<Ticket> guardar(Ticket ticket);
    Flux<Ticket> guardarTodos(Flux<Ticket> tickets);
    Mono<Ticket> buscarPorId(UUID id);
    Flux<Ticket> buscarPorVenta(UUID ventaId);
    Mono<Ticket> actualizarEstado(UUID id, com.ticketseller.domain.model.EstadoTicket estado);
}
