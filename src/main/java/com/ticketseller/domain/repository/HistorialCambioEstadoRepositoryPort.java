package com.ticketseller.domain.repository;

import com.ticketseller.domain.model.asiento.HistorialCambioEstado;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface HistorialCambioEstadoRepositoryPort {
    Mono<HistorialCambioEstado> guardar(HistorialCambioEstado historial);
    Flux<HistorialCambioEstado> findByAsientoId(UUID asientoId);
}
