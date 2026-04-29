package com.ticketseller.infrastructure.adapter.out.persistence.postventa;

import com.ticketseller.domain.model.postventa.EstadoReembolso;
import com.ticketseller.domain.model.postventa.Reembolso;
import com.ticketseller.domain.repository.ReembolsoRepositoryPort;
import com.ticketseller.infrastructure.adapter.out.persistence.postventa.mapper.ReembolsoPersistenceMapper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class ReembolsoRepositoryAdapter implements ReembolsoRepositoryPort {
    private final ReembolsoR2dbcRepository repository;
    private final ReembolsoPersistenceMapper mapper;

    @Override
    public Mono<Reembolso> guardar(Reembolso reembolso) {
        return repository.save(mapper.toEntity(reembolso)).map(mapper::toDomain);
    }

    @Override
    public Flux<Reembolso> guardarTodos(Iterable<Reembolso> reembolsos) {
        return Flux.fromIterable(reembolsos)
                .map(mapper::toEntity)
                .collectList()
                .flatMapMany(repository::saveAll)
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Reembolso> buscarPorId(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Mono<Reembolso> buscarPorTicketId(UUID ticketId) {
        return repository.findFirstByTicketIdOrderByFechaSolicitudDesc(ticketId).map(mapper::toDomain);
    }

    @Override
    public Flux<Reembolso> buscarPorVentaId(UUID ventaId) {
        return repository.findByVentaId(ventaId).map(mapper::toDomain);
    }

    @Override
    public Flux<Reembolso> buscarPorEstado(EstadoReembolso estado) {
        return repository.findByEstado(estado.name()).map(mapper::toDomain);
    }
}

