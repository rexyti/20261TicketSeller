package com.ticketseller.infrastructure.adapter.out.persistence.evento;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.UUID;

public interface EventoR2dbcRepository extends ReactiveCrudRepository<EventoEntity, UUID> {

    Flux<EventoEntity> findByEstado(String estado);

    @Query("""
            SELECT *
            FROM eventos
            WHERE (:estado IS NULL OR estado = :estado)
              AND (:tipo IS NULL OR tipo = :tipo)
              AND (:fechaInicioDesde IS NULL OR fecha_inicio >= :fechaInicioDesde)
              AND (:fechaInicioHasta IS NULL OR fecha_inicio <= :fechaInicioHasta)
            """)
    Flux<EventoEntity> listarConFiltros(String estado, String tipo, LocalDateTime fechaInicioDesde, LocalDateTime fechaInicioHasta);

    @Query("""
            SELECT *
            FROM eventos
            WHERE recinto_id = :recintoId
              AND estado <> 'CANCELADO'
              AND fecha_inicio < :fechaFin
              AND fecha_fin > :fechaInicio
            """)
    Flux<EventoEntity> buscarEventosSolapados(UUID recintoId, LocalDateTime fechaInicio, LocalDateTime fechaFin);
}

