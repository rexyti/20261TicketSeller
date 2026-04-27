package com.ticketseller.infrastructure.adapter.out.persistence.cortesia;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface CortesiaR2dbcRepository extends ReactiveCrudRepository<CortesiaEntity, UUID> {
    Flux<CortesiaEntity> findByEventoId(UUID eventoId);
}
