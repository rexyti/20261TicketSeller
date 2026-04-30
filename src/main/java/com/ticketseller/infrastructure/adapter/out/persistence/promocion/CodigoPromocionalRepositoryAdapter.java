package com.ticketseller.infrastructure.adapter.out.persistence.promocion;

import com.ticketseller.domain.model.promocion.CodigoPromocional;
import com.ticketseller.domain.repository.CodigoPromocionalRepositoryPort;
import com.ticketseller.infrastructure.adapter.out.persistence.promocion.mapper.CodigoPromocionalPersistenceMapper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class CodigoPromocionalRepositoryAdapter implements CodigoPromocionalRepositoryPort {

    private final CodigoPromocionalR2dbcRepository repository;
    private final CodigoPromocionalPersistenceMapper mapper;

    @Override
    public Mono<CodigoPromocional> guardar(CodigoPromocional codigoPromocional) {
        return repository.save(mapper.toEntity(codigoPromocional)).map(mapper::toDomain);
    }

    @Override
    public Flux<CodigoPromocional> guardarTodos(Iterable<CodigoPromocional> codigosPromocionales) {
        return Flux.fromIterable(codigosPromocionales)
                .map(mapper::toEntity)
                .collectList()
                .flatMapMany(repository::saveAll)
                .map(mapper::toDomain);
    }

    @Override
    public Mono<CodigoPromocional> buscarPorCodigo(String codigo) {
        return repository.buscarPorCodigo(codigo).map(mapper::toDomain);
    }

    @Override
    public Mono<Boolean> incrementarUsoAtomico(String codigo, LocalDateTime fecha) {
        return repository.incrementarUsoAtomico(codigo, fecha).map(rows -> rows != null && rows > 0);
    }
}

