package com.ticketseller.domain.repository;

import com.ticketseller.domain.model.promocion.Promocion;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface PromocionRepositoryPort {

    Mono<Promocion> guardar(Promocion promocion);

    Mono<Promocion> buscarPorId(UUID id);

    Flux<Promocion> buscarActivasPorEvento(UUID eventoId);
}
