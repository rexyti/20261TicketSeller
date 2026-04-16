package com.ticketseller.infrastructure.adapter.out.persistence.zona;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ZonaR2dbcRepository extends ReactiveCrudRepository<ZonaEntity, UUID> {

    Flux<ZonaEntity> findByRecintoId(UUID recintoId);

    @Query("SELECT COALESCE(SUM(capacidad), 0) FROM zonas WHERE recinto_id = :recintoId")
    Mono<Integer> sumarCapacidadesPorRecinto(UUID recintoId);
}

