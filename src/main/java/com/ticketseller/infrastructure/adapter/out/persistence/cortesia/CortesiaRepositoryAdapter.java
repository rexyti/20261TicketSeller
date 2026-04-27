package com.ticketseller.infrastructure.adapter.out.persistence.cortesia;

import com.ticketseller.domain.model.Cortesia;
import com.ticketseller.domain.repository.CortesiaRepositoryPort;
import com.ticketseller.infrastructure.adapter.out.persistence.cortesia.mapper.CortesiaPersistenceMapper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class CortesiaRepositoryAdapter implements CortesiaRepositoryPort {

    private final CortesiaR2dbcRepository repository;
    private final CortesiaPersistenceMapper mapper;

    @Override
    public Mono<Cortesia> guardar(Cortesia cortesia) {
        return repository.save(mapper.toEntity(cortesia)).map(mapper::toDomain);
    }

    @Override
    public Mono<Cortesia> buscarPorId(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Flux<Cortesia> buscarPorEventoId(UUID eventoId) {
        return repository.findByEventoId(eventoId).map(mapper::toDomain);
    }
}
