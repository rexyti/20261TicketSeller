package com.ticketseller.infrastructure.adapter.out.persistence.cancelacionevento;

import com.ticketseller.domain.model.CancelacionEvento;
import com.ticketseller.domain.repository.CancelacionEventoRepositoryPort;
import com.ticketseller.infrastructure.adapter.out.persistence.cancelacionevento.mapper.CancelacionEventoPersistenceMapper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class CancelacionEventoRepositoryAdapter implements CancelacionEventoRepositoryPort {

    private final CancelacionEventoR2dbcRepository repository;
    private final CancelacionEventoPersistenceMapper mapper;

    @Override
    public Mono<CancelacionEvento> guardar(CancelacionEvento cancelacionEvento) {
        return repository.save(mapper.toEntity(cancelacionEvento)).map(mapper::toDomain);
    }
}

