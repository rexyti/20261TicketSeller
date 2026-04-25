package com.ticketseller.infrastructure.adapter.out.persistence;

import com.ticketseller.domain.model.HistorialCambioEstado;
import com.ticketseller.domain.repository.HistorialCambioEstadoRepositoryPort;
import com.ticketseller.infrastructure.adapter.out.persistence.mapper.HistorialCambioEstadoPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class HistorialCambioEstadoRepositoryAdapter implements HistorialCambioEstadoRepositoryPort {

    private final HistorialCambioEstadoR2dbcRepository repository;
    private final HistorialCambioEstadoPersistenceMapper mapper;

    @Override
    public Mono<HistorialCambioEstado> guardar(HistorialCambioEstado historial) {
        return Mono.just(historial)
                .map(mapper::toEntity)
                .flatMap(repository::save)
                .map(mapper::toDomain);
    }

    @Override
    public Flux<HistorialCambioEstado> findByAsientoId(UUID asientoId) {
        return repository.findByAsientoIdOrderByFechaHoraDesc(asientoId)
                .map(mapper::toDomain);
    }
}
