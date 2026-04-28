package com.ticketseller.domain.repository;

import com.ticketseller.domain.model.zona.Compuerta;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CompuertaRepositoryPort {

    Mono<Compuerta> guardar(Compuerta compuerta);

    Flux<Compuerta> buscarPorRecintoId(UUID recintoId);

    Flux<Compuerta> buscarPorZonaId(UUID zonaId);

    Mono<Compuerta> buscarPorId(UUID id);
}

