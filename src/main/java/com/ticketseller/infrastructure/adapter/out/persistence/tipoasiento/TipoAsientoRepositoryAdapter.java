package com.ticketseller.infrastructure.adapter.out.persistence.tipoasiento;

import com.ticketseller.domain.model.asiento.TipoAsiento;
import com.ticketseller.domain.repository.TipoAsientoRepositoryPort;
import com.ticketseller.infrastructure.adapter.out.persistence.tipoasiento.mapper.TipoAsientoPersistenceMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class TipoAsientoRepositoryAdapter implements TipoAsientoRepositoryPort {

    private final TipoAsientoR2dbcRepository repository;
    private final TipoAsientoPersistenceMapper mapper;

    public TipoAsientoRepositoryAdapter(TipoAsientoR2dbcRepository repository,
                                        TipoAsientoPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Mono<TipoAsiento> guardar(TipoAsiento tipoAsiento) {
        return repository.save(mapper.toEntity(tipoAsiento)).map(mapper::toDomain);
    }

    @Override
    public Mono<TipoAsiento> buscarPorId(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Mono<TipoAsiento> buscarPorNombre(String nombre) {
        return repository.findByNombre(nombre).map(mapper::toDomain);
    }

    @Override
    public Flux<TipoAsiento> listarTodos() {
        return repository.findAll().map(mapper::toDomain);
    }

    @Override
    public Mono<Boolean> tieneEventosFuturos(UUID tipoAsientoId) {
        return Mono.just(false);
    }

    @Override
    public Mono<Boolean> tieneAsignacionEnZona(UUID tipoAsientoId) {
        return repository.tieneAsignacionEnZona(tipoAsientoId);
    }
}
