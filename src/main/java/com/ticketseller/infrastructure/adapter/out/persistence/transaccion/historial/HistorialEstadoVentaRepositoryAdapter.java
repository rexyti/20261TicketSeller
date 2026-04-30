package com.ticketseller.infrastructure.adapter.out.persistence.transaccion.historial;

import com.ticketseller.domain.model.transaccion.HistorialEstadoVenta;
import com.ticketseller.domain.repository.HistorialEstadoVentaRepositoryPort;
import com.ticketseller.infrastructure.adapter.out.persistence.transaccion.historial.mapper.HistorialEstadoVentaPersistenceMapper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class HistorialEstadoVentaRepositoryAdapter implements HistorialEstadoVentaRepositoryPort {

    private final HistorialEstadoVentaR2dbcRepository repository;
    private final HistorialEstadoVentaPersistenceMapper mapper;

    @Override
    public Mono<HistorialEstadoVenta> guardar(HistorialEstadoVenta historial) {
        return repository.save(mapper.toEntity(historial)).map(mapper::toDomain);
    }

    @Override
    public Flux<HistorialEstadoVenta> buscarPorVentaId(UUID ventaId) {
        return repository.findByVentaIdOrderByFechaCambioAsc(ventaId).map(mapper::toDomain);
    }
}
