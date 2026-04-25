package com.ticketseller.domain.repository;

import com.ticketseller.domain.model.HistorialTicket;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface HistorialTicketRepositoryPort {
    Mono<HistorialTicket> save(HistorialTicket historial);
    Flux<HistorialTicket> findByTicketId(UUID ticketId);
}
