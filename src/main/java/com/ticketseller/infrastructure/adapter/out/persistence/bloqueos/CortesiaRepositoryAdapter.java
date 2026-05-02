package com.ticketseller.infrastructure.adapter.out.persistence.bloqueos;

import com.ticketseller.domain.model.bloqueos.Cortesia;
import com.ticketseller.domain.repository.CortesiaRepositoryPort;
import com.ticketseller.infrastructure.adapter.out.persistence.bloqueos.mapper.CortesiaPersistenceMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class CortesiaRepositoryAdapter implements CortesiaRepositoryPort {

    private final CortesiaR2dbcRepository repository;
    private final CortesiaPersistenceMapper mapper;

    public CortesiaRepositoryAdapter(CortesiaR2dbcRepository repository, CortesiaPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Mono<Cortesia> guardar(Cortesia cortesia) {
        return repository.save(mapper.toEntity(cortesia)).map(mapper::toDomain);
    }

    @Override
    public Mono<Cortesia> buscarPorId(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Flux<Cortesia> buscarPorEvento(UUID eventoId) {
        return repository.findByEventoId(eventoId).map(mapper::toDomain);
    }
}
