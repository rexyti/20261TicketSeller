package com.ticketseller.infrastructure.adapter.out.persistence.promocion;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

public interface CodigoPromocionalR2dbcRepository extends ReactiveCrudRepository<CodigoPromocionalEntity, UUID> {

    @Query("SELECT * FROM codigos_promocionales WHERE UPPER(codigo) = UPPER(:codigo)")
    Mono<CodigoPromocionalEntity> buscarPorCodigo(String codigo);

    @Query("""
            UPDATE codigos_promocionales
            SET usos_actuales = usos_actuales + 1,
                estado = CASE
                    WHEN usos_maximos IS NOT NULL AND usos_actuales + 1 >= usos_maximos THEN 'AGOTADO'
                    ELSE estado
                END
            WHERE UPPER(codigo) = UPPER(:codigo)
              AND estado = 'ACTIVO'
              AND fecha_inicio <= :fecha
              AND fecha_fin >= :fecha
              AND (usos_maximos IS NULL OR usos_actuales < usos_maximos)
            """)
    Mono<Integer> incrementarUsoAtomico(String codigo, LocalDateTime fecha);
}

