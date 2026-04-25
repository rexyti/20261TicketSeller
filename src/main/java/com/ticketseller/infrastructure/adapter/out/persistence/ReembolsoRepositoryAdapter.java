package com.ticketseller.infrastructure.adapter.out.persistence;

import com.ticketseller.domain.model.EstadoReembolso;
import com.ticketseller.domain.model.Reembolso;
import com.ticketseller.domain.repository.ReembolsoRepositoryPort;
import com.ticketseller.infrastructure.adapter.out.persistence.mapper.ReembolsoPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class ReembolsoRepositoryAdapter implements ReembolsoRepositoryPort {

    private final ReembolsoR2dbcRepository repository;
    private final ReembolsoPersistenceMapper mapper;

    @Override
    public Mono<Reembolso> save(Reembolso reembolso) {
        return repository.save(mapper.toEntity(reembolso))
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Reembolso> findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Reembolso> findByTicketId(UUID ticketId) {
        return repository.findByTicketId(ticketId)
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Reembolso> findByVentaId(UUID ventaId) {
        return repository.findByVentaId(ventaId)
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Reembolso> findByEstado(EstadoReembolso estado) {
        return repository.findByEstado(estado.name())
                .map(mapper::toDomain);
    }
}
