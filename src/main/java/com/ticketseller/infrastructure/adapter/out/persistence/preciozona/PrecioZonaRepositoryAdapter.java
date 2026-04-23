package com.ticketseller.infrastructure.adapter.out.persistence.preciozona;

import com.ticketseller.domain.model.PrecioZona;
import com.ticketseller.domain.repository.PrecioZonaRepositoryPort;
import com.ticketseller.infrastructure.adapter.out.persistence.preciozona.mapper.PrecioZonaPersistenceMapper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class PrecioZonaRepositoryAdapter implements PrecioZonaRepositoryPort {

    private final PrecioZonaR2dbcRepository repository;
    private final PrecioZonaPersistenceMapper mapper;

    @Override
    public Mono<PrecioZona> guardar(PrecioZona precioZona) {
        return repository.save(mapper.toEntity(precioZona)).map(mapper::toDomain);
    }

    @Override
    public Flux<PrecioZona> buscarPorEvento(UUID eventoId) {
        return repository.findByEventoId(eventoId).map(mapper::toDomain);
    }

    @Override
    public Mono<Void> eliminarPorEvento(UUID eventoId) {
        return repository.deleteByEventoId(eventoId);
    }
}

