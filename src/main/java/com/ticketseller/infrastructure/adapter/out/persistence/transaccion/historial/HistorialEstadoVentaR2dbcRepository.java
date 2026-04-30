package com.ticketseller.infrastructure.adapter.out.persistence.transaccion.historial;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface HistorialEstadoVentaR2dbcRepository
        extends ReactiveCrudRepository<HistorialEstadoVentaEntity, UUID> {

    Flux<HistorialEstadoVentaEntity> findByVentaIdOrderByFechaCambioAsc(UUID ventaId);
}
