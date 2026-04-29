package com.ticketseller.infrastructure.adapter.out.persistence.postventa;

import com.ticketseller.domain.model.postventa.HistorialEstadoTicket;
import com.ticketseller.domain.repository.HistorialEstadoTicketRepositoryPort;
import com.ticketseller.infrastructure.adapter.out.persistence.postventa.mapper.HistorialEstadoTicketPersistenceMapper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class HistorialEstadoTicketRepositoryAdapter implements HistorialEstadoTicketRepositoryPort {
    private final HistorialEstadoTicketR2dbcRepository repository;
    private final HistorialEstadoTicketPersistenceMapper mapper;

    @Override
    public Mono<HistorialEstadoTicket> guardar(HistorialEstadoTicket historial) {
        return repository.save(mapper.toEntity(historial)).map(mapper::toDomain);
    }
}

