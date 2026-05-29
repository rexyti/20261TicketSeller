package com.ticketseller.infrastructure.adapter.out.persistence.bloqueos;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface BloqueoR2dbcRepository extends R2dbcRepository<BloqueoEntity, UUID> {
    Flux<BloqueoEntity> findByEventoId(UUID eventoId);

    Flux<BloqueoEntity> findByEventoIdAndEstado(UUID eventoId, String estado);

    @Query("SELECT * FROM bloqueos WHERE asiento_id = :asientoId AND evento_id = :eventoId AND estado = 'ACTIVO' LIMIT 1")
    Mono<BloqueoEntity> findActiveByAsientoIdAndEventoId(UUID asientoId, UUID eventoId);
}
