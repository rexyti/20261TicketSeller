package com.ticketseller.infrastructure.adapter.out.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface HistorialTicketR2dbcRepository extends ReactiveCrudRepository<HistorialTicketEntity, UUID> {
    Flux<HistorialTicketEntity> findByTicketId(UUID ticketId);
}
