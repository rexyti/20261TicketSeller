package com.ticketseller.infrastructure.adapter.out.persistence.compuerta;

import com.ticketseller.domain.model.Compuerta;
import com.ticketseller.domain.port.out.CompuertaRepositoryPort;
import com.ticketseller.infrastructure.adapter.out.persistence.compuerta.mapper.CompuertaPersistenceMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class CompuertaRepositoryAdapter implements CompuertaRepositoryPort {

    private final CompuertaR2dbcRepository repository;
    private final CompuertaPersistenceMapper mapper;

    public CompuertaRepositoryAdapter(CompuertaR2dbcRepository repository, CompuertaPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Mono<Compuerta> guardar(Compuerta compuerta) {
        return repository.save(mapper.toEntity(compuerta)).map(mapper::toDomain);
    }

    @Override
    public Flux<Compuerta> buscarPorRecintoId(UUID recintoId) {
        return repository.findByRecintoId(recintoId).map(mapper::toDomain);
    }

    @Override
    public Flux<Compuerta> buscarPorZonaId(UUID zonaId) {
        return repository.findByZonaId(zonaId).map(mapper::toDomain);
    }

    @Override
    public Mono<Compuerta> buscarPorId(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }
}

