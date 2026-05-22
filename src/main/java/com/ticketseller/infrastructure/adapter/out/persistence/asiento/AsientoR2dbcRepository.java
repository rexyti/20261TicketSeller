package com.ticketseller.infrastructure.adapter.out.persistence.asiento;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface AsientoR2dbcRepository extends ReactiveCrudRepository<AsientoEntity, UUID> {
    Flux<AsientoEntity> findByZonaId(UUID zonaId);

    @Query("SELECT * FROM asientos WHERE recinto_id = :recintoId")
    Flux<AsientoEntity> findByRecintoId(UUID recintoId);

    @Query("SELECT a.* FROM asientos a JOIN eventos e ON a.recinto_id = e.recinto_id WHERE e.id = :eventoId")
    Flux<AsientoEntity> findByEventoId(UUID eventoId);
}
