package com.ticketseller.infrastructure.adapter.out.persistence.checkout;

import com.ticketseller.domain.model.EstadoVenta;
import com.ticketseller.domain.model.Venta;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import com.ticketseller.infrastructure.adapter.out.persistence.checkout.mapper.VentaPersistenceMapper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class VentaRepositoryAdapter implements VentaRepositoryPort {

    private final VentaR2dbcRepository repository;
    private final VentaPersistenceMapper mapper;

    @Override
    public Mono<Venta> guardar(Venta venta) {
        return repository.save(mapper.toEntity(venta)).map(mapper::toDomain);
    }

    @Override
    public Mono<Venta> buscarPorId(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Flux<Venta> buscarVentasExpiradas(LocalDateTime fechaCorte) {
        return repository.findByEstadoAndFechaExpiracionBefore(EstadoVenta.RESERVADA.name(), fechaCorte)
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Venta> actualizarEstado(UUID id, EstadoVenta estado) {
        return repository.findById(id)
                .map(entity -> entity.toBuilder().estado(estado.name()).build())
                .flatMap(repository::save)
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Venta> buscarPorComprador(UUID compradorId) {
        return repository.findByCompradorId(compradorId).map(mapper::toDomain);
    }
}

