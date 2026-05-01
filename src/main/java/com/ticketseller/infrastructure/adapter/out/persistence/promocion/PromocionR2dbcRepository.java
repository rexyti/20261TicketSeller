package com.ticketseller.infrastructure.adapter.out.persistence.promocion;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface PromocionR2dbcRepository extends ReactiveCrudRepository<PromocionEntity, UUID> {

    @Query("SELECT * FROM promociones WHERE evento_id = :eventoId AND estado = 'ACTIVA'")
    Flux<PromocionEntity> findActivasByEventoId(UUID eventoId);
}
