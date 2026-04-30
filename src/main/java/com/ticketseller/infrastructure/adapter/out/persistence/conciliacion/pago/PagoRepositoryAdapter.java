package com.ticketseller.infrastructure.adapter.out.persistence.conciliacion.pago;

import com.ticketseller.domain.model.conciliacion.EstadoConciliacion;
import com.ticketseller.domain.model.conciliacion.Pago;
import com.ticketseller.domain.repository.PagoRepositoryPort;
import com.ticketseller.infrastructure.adapter.out.persistence.conciliacion.pago.mapper.PagoPersistenceMapper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class PagoRepositoryAdapter implements PagoRepositoryPort {

    private final PagoR2dbcRepository repository;
    private final PagoPersistenceMapper mapper;

    @Override
    public Mono<Pago> guardar(Pago pago) {
        return repository.save(mapper.toEntity(pago)).map(mapper::toDomain);
    }

    @Override
    public Mono<Pago> buscarPorId(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Mono<Pago> buscarPorIdExterno(String idExternoPasarela) {
        return repository.findByIdExternoPasarela(idExternoPasarela).map(mapper::toDomain);
    }

    @Override
    public Mono<Pago> buscarPorVentaId(UUID ventaId) {
        return repository.findByVentaId(ventaId).map(mapper::toDomain);
    }

    @Override
    public Flux<Pago> buscarPorEstado(EstadoConciliacion estado) {
        return repository.findByEstado(estado.name()).map(mapper::toDomain);
    }

    @Override
    public Flux<Pago> buscarPendientesAnterioresA(LocalDateTime fechaCorte) {
        return repository.findByEstadoAndFechaCreacionBefore(EstadoConciliacion.PENDIENTE.name(), fechaCorte)
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Pago> actualizarEstado(UUID id, EstadoConciliacion estado) {
        return repository.findById(id)
                .map(entity -> entity.toBuilder()
                        .estado(estado.name())
                        .fechaActualizacion(LocalDateTime.now())
                        .build())
                .flatMap(repository::save)
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Pago> actualizarConResolucion(UUID id, EstadoConciliacion estado, UUID agenteId, String justificacion) {
        return repository.findById(id)
                .map(entity -> entity.toBuilder()
                        .estado(estado.name())
                        .agenteId(agenteId)
                        .justificacionResolucion(justificacion)
                        .fechaActualizacion(LocalDateTime.now())
                        .build())
                .flatMap(repository::save)
                .map(mapper::toDomain);
    }
}
