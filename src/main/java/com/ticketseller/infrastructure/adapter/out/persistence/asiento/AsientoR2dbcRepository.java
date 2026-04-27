package com.ticketseller.infrastructure.adapter.out.persistence.asiento;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import org.springframework.data.r2dbc.repository.Query;
import java.time.LocalDateTime;
import java.util.UUID;

public interface AsientoR2dbcRepository extends ReactiveCrudRepository<AsientoEntity, UUID> {
    Flux<AsientoEntity> findByZonaId(UUID zonaId);

    @Query("SELECT * FROM asientos WHERE estado = 'RESERVADO' AND expira_en < :ahora")
    Flux<AsientoEntity> findHoldsVencidos(LocalDateTime ahora);
}
