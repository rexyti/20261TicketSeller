package com.ticketseller.infrastructure.adapter.out.persistence.checkout;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.UUID;

public interface TicketR2dbcRepository extends ReactiveCrudRepository<TicketEntity, UUID> {

    Flux<TicketEntity> findByVentaId(UUID ventaId);

    Mono<Long> countByEventoIdAndZonaIdAndEstadoIn(UUID eventoId, UUID zonaId, Collection<String> estados);
}


