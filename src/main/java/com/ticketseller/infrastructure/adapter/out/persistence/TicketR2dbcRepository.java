package com.ticketseller.infrastructure.adapter.out.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface TicketR2dbcRepository extends ReactiveCrudRepository<TicketEntity, UUID> {
    Flux<TicketEntity> findByVentaId(UUID ventaId);
}
