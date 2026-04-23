package com.ticketseller.infrastructure.adapter.out.persistence.checkout;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.UUID;

public interface VentaR2dbcRepository extends ReactiveCrudRepository<VentaEntity, UUID> {

    Flux<VentaEntity> findByEstadoAndFechaExpiracionBefore(String estado, LocalDateTime fechaCorte);
}

