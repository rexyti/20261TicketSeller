package com.ticketseller.infrastructure.adapter.out.persistence.promocion;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.UUID;

public interface PromocionR2dbcRepository extends ReactiveCrudRepository<PromocionEntity, UUID> {

    Flux<PromocionEntity> findByEventoIdAndEstadoAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
            UUID eventoId, String estado, LocalDateTime fechaInicio, LocalDateTime fechaFin);

    Flux<PromocionEntity> findByEventoIdAndTipoAndEstadoAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
            UUID eventoId, String tipo, String estado, LocalDateTime fechaInicio, LocalDateTime fechaFin);
}

