package com.ticketseller.infrastructure.adapter.out.persistence.promocion;

import com.ticketseller.domain.model.promocion.Promocion;
import com.ticketseller.domain.repository.PromocionRepositoryPort;
import com.ticketseller.infrastructure.adapter.out.persistence.promocion.mapper.PromocionPersistenceMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class PromocionRepositoryAdapter implements PromocionRepositoryPort {

    private final PromocionR2dbcRepository repository;
    private final PromocionPersistenceMapper mapper;

    public PromocionRepositoryAdapter(PromocionR2dbcRepository repository, PromocionPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Mono<Promocion> guardar(Promocion promocion) {
        return repository.save(mapper.toEntity(promocion)).map(mapper::toDomain);
    }

    @Override
    public Mono<Promocion> buscarPorId(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Flux<Promocion> buscarActivasPorEvento(UUID eventoId) {
        return repository.findActivasByEventoId(eventoId).map(mapper::toDomain);
    }
}
