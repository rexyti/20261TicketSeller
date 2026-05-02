package com.ticketseller.infrastructure.adapter.out.persistence.bloqueos;

import com.ticketseller.domain.model.bloqueos.Bloqueo;
import com.ticketseller.domain.model.bloqueos.EstadoBloqueo;
import com.ticketseller.domain.repository.BloqueoRepositoryPort;
import com.ticketseller.infrastructure.adapter.out.persistence.bloqueos.mapper.BloqueoPersistenceMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class BloqueoRepositoryAdapter implements BloqueoRepositoryPort {

    private final BloqueoR2dbcRepository repository;
    private final BloqueoPersistenceMapper mapper;

    public BloqueoRepositoryAdapter(BloqueoR2dbcRepository repository, BloqueoPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Mono<Bloqueo> guardar(Bloqueo bloqueo) {
        return repository.save(mapper.toEntity(bloqueo)).map(mapper::toDomain);
    }

    @Override
    public Mono<Bloqueo> buscarPorId(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Flux<Bloqueo> buscarPorEvento(UUID eventoId) {
        return repository.findByEventoId(eventoId).map(mapper::toDomain);
    }

    @Override
    public Flux<Bloqueo> buscarPorEventoYEstado(UUID eventoId, EstadoBloqueo estado) {
        return repository.findByEventoIdAndEstado(eventoId, estado.name()).map(mapper::toDomain);
    }
}
