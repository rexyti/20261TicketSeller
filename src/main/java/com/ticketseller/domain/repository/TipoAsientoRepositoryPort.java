package com.ticketseller.domain.repository;

import com.ticketseller.domain.model.asiento.TipoAsiento;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface TipoAsientoRepositoryPort {
    Mono<TipoAsiento> guardar(TipoAsiento tipoAsiento);
    Mono<TipoAsiento> buscarPorId(UUID id);
    Mono<TipoAsiento> buscarPorNombre(String nombre);
    Flux<TipoAsiento> listarTodos();
    Mono<Boolean> tieneEventosFuturos(UUID tipoAsientoId);
    Mono<Boolean> tieneAsignacionEnZona(UUID tipoAsientoId);
}
