package com.ticketseller.domain.repository;

import com.ticketseller.domain.model.EstadoTicket;
import com.ticketseller.domain.model.Ticket;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.UUID;

public interface TicketRepositoryPort {

    Mono<Ticket> guardar(Ticket ticket);

    Flux<Ticket> guardarTodos(Iterable<Ticket> tickets);

    Mono<Ticket> buscarPorId(UUID id);
    
    default Mono<Ticket> findById(UUID ticketId) {
        return buscarPorId(ticketId);
    }

    Flux<Ticket> buscarPorVenta(UUID ventaId);

    Mono<Long> contarPorEventoYZonaYEstados(UUID eventoId, UUID zonaId, Set<EstadoTicket> estados);

    Mono<Void> actualizarEstadoPorVenta(UUID ventaId, EstadoTicket estado);
}

