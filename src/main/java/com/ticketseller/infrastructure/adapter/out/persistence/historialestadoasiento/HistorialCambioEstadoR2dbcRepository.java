package com.ticketseller.infrastructure.adapter.out.persistence.historialestadoasiento;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface HistorialCambioEstadoR2dbcRepository extends ReactiveCrudRepository<HistorialCambioEstadoEntity, UUID> {
    Flux<HistorialCambioEstadoEntity> findByAsientoIdOrderByFechaHoraDesc(UUID asientoId);
}
