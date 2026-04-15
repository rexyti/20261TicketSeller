package com.ticketseller.infrastructure.adapter.out.persistence;

import com.ticketseller.domain.model.EstadoVenta;
import com.ticketseller.domain.model.Venta;
import com.ticketseller.domain.port.out.VentaRepositoryPort;
import com.ticketseller.infrastructure.adapter.out.persistence.mapper.VentaPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VentaRepositoryAdapter implements VentaRepositoryPort {

    private final VentaR2dbcRepository ventaR2dbcRepository;
    private final VentaPersistenceMapper mapper;

    @Override
    public Mono<Venta> guardar(Venta venta) {
        return ventaR2dbcRepository.save(mapper.toEntity(venta))
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Venta> buscarPorId(UUID id) {
        return ventaR2dbcRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Venta> buscarVentasExpiradas(LocalDateTime ahora) {
        return ventaR2dbcRepository.findByFechaExpiracionBeforeAndEstado(ahora, EstadoVenta.RESERVADA.name())
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Venta> actualizarEstado(UUID id, EstadoVenta estado) {
        return ventaR2dbcRepository.findById(id)
                .flatMap(entity -> {
                    entity.setEstado(estado.name());
                    return ventaR2dbcRepository.save(entity);
                })
                .map(mapper::toDomain);
    }
}
