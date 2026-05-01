package com.ticketseller.infrastructure.adapter.out.persistence.promocion;

import com.ticketseller.domain.model.promocion.Descuento;
import com.ticketseller.domain.repository.DescuentoRepositoryPort;
import com.ticketseller.infrastructure.adapter.out.persistence.promocion.mapper.DescuentoPersistenceMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

public class DescuentoRepositoryAdapter implements DescuentoRepositoryPort {

    private final DescuentoR2dbcRepository repository;
    private final DescuentoPersistenceMapper mapper;

    public DescuentoRepositoryAdapter(DescuentoR2dbcRepository repository, DescuentoPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Mono<Descuento> guardar(Descuento descuento) {
        return repository.save(mapper.toEntity(descuento)).map(mapper::toDomain);
    }

    @Override
    public Flux<Descuento> buscarPorPromocionId(UUID promocionId) {
        return repository.findByPromocionId(promocionId).map(mapper::toDomain);
    }

    @Override
    public Flux<Descuento> buscarActivosPorEvento(UUID eventoId, LocalDateTime ahora) {
        return repository.findActivosByEventoIdAndFecha(eventoId, ahora).map(mapper::toDomain);
    }
}
