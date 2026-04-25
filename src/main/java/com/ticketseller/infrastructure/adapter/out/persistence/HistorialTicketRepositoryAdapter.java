package com.ticketseller.infrastructure.adapter.out.persistence;

import com.ticketseller.domain.model.HistorialTicket;
import com.ticketseller.domain.repository.HistorialTicketRepositoryPort;
import com.ticketseller.infrastructure.adapter.out.persistence.mapper.HistorialTicketPersistenceMapper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class HistorialTicketRepositoryAdapter implements HistorialTicketRepositoryPort {

    private final HistorialTicketR2dbcRepository repository;
    private final HistorialTicketPersistenceMapper mapper;

    @Override
    public Mono<HistorialTicket> save(HistorialTicket historial) {
        return repository.save(mapper.toEntity(historial))
                .map(mapper::toDomain);
    }

    @Override
    public Flux<HistorialTicket> findByTicketId(UUID ticketId) {
        return repository.findByTicketId(ticketId)
                .map(mapper::toDomain);
    }
}
