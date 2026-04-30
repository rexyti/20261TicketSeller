package com.ticketseller.infrastructure.adapter.out.persistence.promocion;

import com.ticketseller.domain.model.promocion.Descuento;
import com.ticketseller.domain.repository.DescuentoRepositoryPort;
import com.ticketseller.infrastructure.adapter.out.persistence.promocion.mapper.DescuentoPersistenceMapper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class DescuentoRepositoryAdapter implements DescuentoRepositoryPort {

    private final DescuentoR2dbcRepository repository;
    private final DescuentoPersistenceMapper mapper;

    @Override
    public Mono<Descuento> guardar(Descuento descuento) {
        return repository.save(mapper.toEntity(descuento)).map(mapper::toDomain);
    }

    @Override
    public Flux<Descuento> buscarActivosPorEvento(UUID eventoId, LocalDateTime fecha) {
        return repository.buscarActivosPorEvento(eventoId, fecha).map(mapper::toDomain);
    }

    @Override
    public Flux<Descuento> buscarPorPromocionId(UUID promocionId) {
        return repository.findByPromocionId(promocionId).map(mapper::toDomain);
    }
}

