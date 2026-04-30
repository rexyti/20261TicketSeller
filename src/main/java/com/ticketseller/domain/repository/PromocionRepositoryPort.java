package com.ticketseller.domain.repository;

import com.ticketseller.domain.model.promocion.EstadoPromocion;
import com.ticketseller.domain.model.promocion.Promocion;
import com.ticketseller.domain.model.promocion.TipoPromocion;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

public interface PromocionRepositoryPort {

    Mono<Promocion> guardar(Promocion promocion);

    Mono<Promocion> buscarPorId(UUID id);

    Flux<Promocion> buscarActivasPorEvento(UUID eventoId, TipoPromocion tipo, LocalDateTime fecha);

    Mono<Promocion> actualizarEstado(UUID id, EstadoPromocion estado);
}

