package com.ticketseller.infrastructure.adapter.out.persistence.preciozona;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface PrecioZonaR2dbcRepository extends ReactiveCrudRepository<PrecioZonaEntity, UUID> {

    Flux<PrecioZonaEntity> findByEventoId(UUID eventoId);

    Mono<Void> deleteByEventoId(UUID eventoId);
}

