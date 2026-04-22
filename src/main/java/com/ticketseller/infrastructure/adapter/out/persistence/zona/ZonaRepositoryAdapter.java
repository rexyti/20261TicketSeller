package com.ticketseller.infrastructure.adapter.out.persistence.zona;

import com.ticketseller.domain.model.Zona;
import com.ticketseller.domain.repository.ZonaRepositoryPort;
import com.ticketseller.infrastructure.adapter.out.persistence.zona.mapper.ZonaPersistenceMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class ZonaRepositoryAdapter implements ZonaRepositoryPort {

    private final ZonaR2dbcRepository repository;
    private final ZonaPersistenceMapper mapper;

    public ZonaRepositoryAdapter(ZonaR2dbcRepository repository, ZonaPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Mono<Zona> guardar(Zona zona) {
        return repository.save(mapper.toEntity(zona)).map(mapper::toDomain);
    }

    @Override
    public Flux<Zona> buscarPorRecintoId(UUID recintoId) {
        return repository.findByRecintoId(recintoId).map(mapper::toDomain);
    }

    @Override
    public Mono<Integer> sumarCapacidadesPorRecinto(UUID recintoId) {
        return repository.sumarCapacidadesPorRecinto(recintoId).defaultIfEmpty(0);
    }

    @Override
    public Mono<Zona> buscarPorId(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }
}

