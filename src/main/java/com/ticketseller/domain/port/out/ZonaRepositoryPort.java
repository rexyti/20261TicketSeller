package com.ticketseller.domain.port.out;

import com.ticketseller.domain.model.Zona;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ZonaRepositoryPort {

    Mono<Zona> guardar(Zona zona);

    Flux<Zona> buscarPorRecintoId(UUID recintoId);

    Mono<Integer> sumarCapacidadesPorRecinto(UUID recintoId);

    Mono<Zona> buscarPorId(UUID id);
}

