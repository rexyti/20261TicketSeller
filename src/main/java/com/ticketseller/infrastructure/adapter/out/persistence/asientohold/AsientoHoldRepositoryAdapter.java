package com.ticketseller.infrastructure.adapter.out.persistence.asientohold;

import com.ticketseller.domain.model.asiento.AsientoHold;
import com.ticketseller.domain.repository.AsientoHoldRepositoryPort;
import com.ticketseller.infrastructure.adapter.out.persistence.asientohold.mapper.AsientoHoldPersistenceMapper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class AsientoHoldRepositoryAdapter implements AsientoHoldRepositoryPort {

    private final AsientoHoldR2dbcRepository repository;
    private final AsientoHoldPersistenceMapper mapper;

    @Override
    public Mono<AsientoHold> guardar(AsientoHold hold) {
        return repository.save(mapper.toEntity(hold)).map(mapper::toDomain);
    }

    @Override
    public Mono<AsientoHold> buscarPorId(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Flux<AsientoHold> buscarPorVentaId(UUID ventaId) {
        return repository.findByVentaId(ventaId).map(mapper::toDomain);
    }

    @Override
    public Mono<AsientoHold> buscarActivoPorAsientoId(UUID asientoId) {
        return repository.findActiveByAsientoId(asientoId).map(mapper::toDomain);
    }

    @Override
    public Flux<AsientoHold> buscarHoldsVencidos(LocalDateTime ahora) {
        return repository.findHoldsVencidos(ahora).map(mapper::toDomain);
    }
}
