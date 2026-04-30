package com.ticketseller.infrastructure.adapter.out.persistence.promocion;

import com.ticketseller.domain.model.promocion.EstadoPromocion;
import com.ticketseller.domain.model.promocion.Promocion;
import com.ticketseller.domain.model.promocion.TipoPromocion;
import com.ticketseller.domain.repository.PromocionRepositoryPort;
import com.ticketseller.infrastructure.adapter.out.persistence.promocion.mapper.PromocionPersistenceMapper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class PromocionRepositoryAdapter implements PromocionRepositoryPort {

    private final PromocionR2dbcRepository repository;
    private final PromocionPersistenceMapper mapper;

    @Override
    public Mono<Promocion> guardar(Promocion promocion) {
        return repository.save(mapper.toEntity(promocion)).map(mapper::toDomain);
    }

    @Override
    public Mono<Promocion> buscarPorId(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Flux<Promocion> buscarActivasPorEvento(UUID eventoId, TipoPromocion tipo, LocalDateTime fecha) {
        if (tipo == null) {
            return repository.findByEventoIdAndEstadoAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
                    eventoId, EstadoPromocion.ACTIVA.name(), fecha, fecha
            ).map(mapper::toDomain);
        }
        return repository.findByEventoIdAndTipoAndEstadoAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
                eventoId, tipo.name(), EstadoPromocion.ACTIVA.name(), fecha, fecha
        ).map(mapper::toDomain);
    }

    @Override
    public Mono<Promocion> actualizarEstado(UUID id, EstadoPromocion estado) {
        return repository.findById(id)
                .map(entity -> entity.toBuilder().estado(estado.name()).build())
                .flatMap(repository::save)
                .map(mapper::toDomain);
    }
}

