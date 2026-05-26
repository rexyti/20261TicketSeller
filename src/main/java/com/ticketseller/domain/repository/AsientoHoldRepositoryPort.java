package com.ticketseller.domain.repository;

import com.ticketseller.domain.model.asiento.AsientoHold;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AsientoHoldRepositoryPort {
    Mono<AsientoHold> guardar(AsientoHold hold);
    Mono<AsientoHold> buscarPorId(UUID id);
    Flux<AsientoHold> buscarPorVentaId(UUID ventaId);
    Mono<AsientoHold> buscarActivoPorAsientoYEvento(UUID asientoId, UUID eventoId);
    Flux<AsientoHold> buscarPorAsientoYEvento(UUID asientoId, UUID eventoId);
    Flux<AsientoHold> buscarPorEvento(UUID eventoId);
    Flux<AsientoHold> buscarHoldsVencidos(LocalDateTime ahora);
}
