package com.ticketseller.domain.repository;

import com.ticketseller.domain.model.promocion.Descuento;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

public interface DescuentoRepositoryPort {

    Mono<Descuento> guardar(Descuento descuento);

    Flux<Descuento> buscarActivosPorEvento(UUID eventoId, LocalDateTime fecha);

    Flux<Descuento> buscarPorPromocionId(UUID promocionId);
}

