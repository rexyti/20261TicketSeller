package com.ticketseller.domain.repository;

import com.ticketseller.domain.model.bloqueos.Bloqueo;
import com.ticketseller.domain.model.bloqueos.EstadoBloqueo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface BloqueoRepositoryPort {
    Mono<Bloqueo> guardar(Bloqueo bloqueo);

    Mono<Bloqueo> buscarPorId(UUID id);

    Flux<Bloqueo> buscarPorEvento(UUID eventoId);

    Flux<Bloqueo> buscarPorEventoYEstado(UUID eventoId, EstadoBloqueo estado);
}
