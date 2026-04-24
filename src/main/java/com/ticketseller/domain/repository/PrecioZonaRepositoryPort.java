package com.ticketseller.domain.repository;

import com.ticketseller.domain.model.PrecioZona;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface PrecioZonaRepositoryPort {

    Mono<PrecioZona> guardar(PrecioZona precioZona);

    Flux<PrecioZona> buscarPorEvento(UUID eventoId);

    Mono<Void> eliminarPorEvento(UUID eventoId);
}

