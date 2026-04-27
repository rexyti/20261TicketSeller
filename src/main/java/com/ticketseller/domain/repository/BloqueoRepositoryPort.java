package com.ticketseller.domain.repository;

import com.ticketseller.domain.model.Bloqueo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface BloqueoRepositoryPort {
    Mono<Bloqueo> guardar(Bloqueo bloqueo);
    Flux<Bloqueo> guardarTodos(List<Bloqueo> bloqueos);
    Mono<Bloqueo> buscarPorId(UUID id);
    Flux<Bloqueo> buscarPorEventoId(UUID eventoId);
    Flux<Bloqueo> buscarActivosPorEventoId(UUID eventoId);
    Mono<Bloqueo> buscarActivoPorAsientoId(UUID asientoId);
}
