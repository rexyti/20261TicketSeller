package com.ticketseller.infrastructure.adapter.out.persistence.compuerta;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface CompuertaR2dbcRepository extends ReactiveCrudRepository<CompuertaEntity, UUID> {

    Flux<CompuertaEntity> findByRecintoId(UUID recintoId);

    Flux<CompuertaEntity> findByZonaId(UUID zonaId);
}

