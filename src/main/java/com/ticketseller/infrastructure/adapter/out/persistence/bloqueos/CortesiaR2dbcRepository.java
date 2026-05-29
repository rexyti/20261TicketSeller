package com.ticketseller.infrastructure.adapter.out.persistence.bloqueos;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CortesiaR2dbcRepository extends R2dbcRepository<CortesiaEntity, UUID> {
    Flux<CortesiaEntity> findByEventoId(UUID eventoId);

    Mono<CortesiaEntity> findByEventoIdAndAsientoId(UUID eventoId, UUID asientoId);
}
