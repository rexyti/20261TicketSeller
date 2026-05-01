package com.ticketseller.infrastructure.adapter.out.persistence.promocion;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.UUID;

public interface DescuentoR2dbcRepository extends ReactiveCrudRepository<DescuentoEntity, UUID> {

    Flux<DescuentoEntity> findByPromocionId(UUID promocionId);

    @Query("""
            SELECT d.*
            FROM descuentos d
            JOIN promociones p ON d.promocion_id = p.id
            WHERE p.evento_id = :eventoId
              AND p.estado = 'ACTIVA'
              AND p.fecha_inicio <= :ahora
              AND p.fecha_fin >= :ahora
            """)
    Flux<DescuentoEntity> findActivosByEventoIdAndFecha(UUID eventoId, LocalDateTime ahora);
}
