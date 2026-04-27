package com.ticketseller.infrastructure.adapter.out.persistence.bloqueo;

import com.ticketseller.domain.model.Bloqueo;
import com.ticketseller.domain.model.EstadoBloqueo;
import com.ticketseller.domain.repository.BloqueoRepositoryPort;
import com.ticketseller.infrastructure.adapter.out.persistence.bloqueo.mapper.BloqueoPersistenceMapper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class BloqueoRepositoryAdapter implements BloqueoRepositoryPort {

    private final BloqueoR2dbcRepository repository;
    private final BloqueoPersistenceMapper mapper;

    @Override
    public Mono<Bloqueo> guardar(Bloqueo bloqueo) {
        return repository.save(mapper.toEntity(bloqueo)).map(mapper::toDomain);
    }

    @Override
    public Flux<Bloqueo> guardarTodos(List<Bloqueo> bloqueos) {
        List<BloqueoEntity> entities = bloqueos.stream().map(mapper::toEntity).toList();
        return repository.saveAll(entities).map(mapper::toDomain);
    }

    @Override
    public Mono<Bloqueo> buscarPorId(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Flux<Bloqueo> buscarPorEventoId(UUID eventoId) {
        return repository.findByEventoId(eventoId).map(mapper::toDomain);
    }

    @Override
    public Flux<Bloqueo> buscarActivosPorEventoId(UUID eventoId) {
        return repository.findByEventoIdAndEstado(eventoId, EstadoBloqueo.ACTIVO.name())
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Bloqueo> buscarActivoPorAsientoId(UUID asientoId) {
        return repository.findByAsientoIdAndEstado(asientoId, EstadoBloqueo.ACTIVO.name())
                .map(mapper::toDomain);
    }
}
