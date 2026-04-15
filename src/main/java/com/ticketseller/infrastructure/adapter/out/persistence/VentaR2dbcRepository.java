package com.ticketseller.infrastructure.adapter.out.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

public interface VentaR2dbcRepository extends ReactiveCrudRepository<VentaEntity, UUID> {
    Flux<VentaEntity> findByFechaExpiracionBeforeAndEstado(LocalDateTime fechaExpiracion, String estado);
    Mono<VentaEntity> findByIdAndEstado(UUID id, String estado);
}
