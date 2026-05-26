package com.ticketseller.infrastructure.adapter.out.persistence.asientohold;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AsientoHoldR2dbcRepository extends ReactiveCrudRepository<AsientoHoldEntity, UUID> {
    Flux<AsientoHoldEntity> findByVentaId(UUID ventaId);

    @Query("SELECT * FROM asiento_holds WHERE asiento_id = :asientoId AND evento_id = :eventoId AND estado = 'RESERVADO' LIMIT 1")
    Mono<AsientoHoldEntity> findActiveByAsientoIdAndEventoId(UUID asientoId, UUID eventoId);

    @Query("SELECT * FROM asiento_holds WHERE asiento_id = :asientoId AND evento_id = :eventoId")
    Flux<AsientoHoldEntity> findByAsientoIdAndEventoId(UUID asientoId, UUID eventoId);

    @Query("SELECT * FROM asiento_holds WHERE evento_id = :eventoId")
    Flux<AsientoHoldEntity> findByEventoId(UUID eventoId);

    @Query("SELECT * FROM asiento_holds WHERE estado = 'RESERVADO' AND expira_en < :ahora")
    Flux<AsientoHoldEntity> findHoldsVencidos(LocalDateTime ahora);
}
