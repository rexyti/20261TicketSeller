package com.ticketseller.infrastructure.adapter.out.persistence.tipoasiento;

import com.ticketseller.domain.model.asiento.TipoAsiento;
import com.ticketseller.domain.repository.TipoAsientoRepositoryPort;
import com.ticketseller.infrastructure.adapter.out.persistence.tipoasiento.mapper.TipoAsientoPersistenceMapper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class TipoAsientoRepositoryAdapter implements TipoAsientoRepositoryPort {

    private final TipoAsientoR2dbcRepository repository;
    private final TipoAsientoPersistenceMapper mapper;

    @Override
    public Mono<TipoAsiento> guardar(TipoAsiento tipoAsiento) {
        TipoAsientoEntity entity = mapper.toEntity(tipoAsiento);
        if (entity.getId() != null) {
            return repository.findById(entity.getId())
                    .map(existing -> entity.toBuilder().createdAt(existing.getCreatedAt()).build())
                    .defaultIfEmpty(entity)
                    .flatMap(repository::save)
                    .map(mapper::toDomain);
        }
        return repository.save(entity).map(mapper::toDomain);
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
