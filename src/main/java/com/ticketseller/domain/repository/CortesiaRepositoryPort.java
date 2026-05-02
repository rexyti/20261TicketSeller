package com.ticketseller.domain.repository;

import com.ticketseller.domain.model.bloqueos.Cortesia;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CortesiaRepositoryPort {
    Mono<Cortesia> guardar(Cortesia cortesia);

    Mono<Cortesia> buscarPorId(UUID id);

    Flux<Cortesia> buscarPorEvento(UUID eventoId);
}
