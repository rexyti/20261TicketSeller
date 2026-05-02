package com.ticketseller.infrastructure.adapter.out.persistence.bloqueos;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface BloqueoR2dbcRepository extends R2dbcRepository<BloqueoEntity, UUID> {
    Flux<BloqueoEntity> findByEventoId(UUID eventoId);

    Flux<BloqueoEntity> findByEventoIdAndEstado(UUID eventoId, String estado);
}
