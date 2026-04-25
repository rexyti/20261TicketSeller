package com.ticketseller.infrastructure.adapter.out.persistence.tipoasiento;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface TipoAsientoR2dbcRepository extends ReactiveCrudRepository<TipoAsientoEntity, UUID> {
    Mono<TipoAsientoEntity> findByNombre(String nombre);

    @Query("SELECT EXISTS(SELECT 1 FROM zonas WHERE tipo_asiento_id = :tipoAsientoId)")
    Mono<Boolean> tieneAsignacionEnZona(UUID tipoAsientoId);
}
