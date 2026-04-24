package com.ticketseller.domain.repository;

import com.ticketseller.domain.model.Asiento;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface AsientoRepositoryPort {
    Mono<Asiento> guardar(Asiento asiento);
    Flux<Asiento> guardarTodos(List<Asiento> asientos);
    Mono<Asiento> buscarPorId(UUID id);
    Flux<Asiento> buscarPorZonaId(UUID zonaId);
}
