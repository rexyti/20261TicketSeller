package com.ticketseller.infrastructure.adapter.out.persistence.bloqueo;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface BloqueoR2dbcRepository extends ReactiveCrudRepository<BloqueoEntity, UUID> {
    Flux<BloqueoEntity> findByEventoId(UUID eventoId);
    Flux<BloqueoEntity> findByEventoIdAndEstado(UUID eventoId, String estado);
    Mono<BloqueoEntity> findByAsientoIdAndEstado(UUID asientoId, String estado);
}
