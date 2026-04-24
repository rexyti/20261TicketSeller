package com.ticketseller.infrastructure.adapter.out.persistence.asiento;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface AsientoR2dbcRepository extends ReactiveCrudRepository<AsientoEntity, UUID> {
    Flux<AsientoEntity> findByZonaId(UUID zonaId);
}
