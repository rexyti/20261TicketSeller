package com.ticketseller.infrastructure.adapter.out.persistence.checkout;

import com.ticketseller.domain.model.EstadoTicket;
import com.ticketseller.domain.model.Ticket;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import com.ticketseller.infrastructure.adapter.out.persistence.checkout.mapper.TicketPersistenceMapper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
public class TicketRepositoryAdapter implements TicketRepositoryPort {

    private final TicketR2dbcRepository repository;
    private final TicketPersistenceMapper mapper;

    @Override
    public Mono<Ticket> guardar(Ticket ticket) {
        return repository.save(mapper.toEntity(ticket)).map(mapper::toDomain);
    }

    @Override
    public Flux<Ticket> guardarTodos(Iterable<Ticket> tickets) {
        return Flux.fromIterable(tickets)
                .map(mapper::toEntity)
                .collectList()
                .flatMapMany(repository::saveAll)
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Ticket> buscarPorId(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Flux<Ticket> buscarPorVenta(UUID ventaId) {
        return repository.findByVentaId(ventaId).map(mapper::toDomain);
    }

    @Override
    public Mono<Long> contarPorEventoYZonaYEstados(UUID eventoId, UUID zonaId, Set<EstadoTicket> estados) {
        return repository.countByEventoIdAndZonaIdAndEstadoIn(eventoId, zonaId,
                estados.stream().map(Enum::name).toList());
    }

    @Override
    public Mono<Void> actualizarEstadoPorVenta(UUID ventaId, EstadoTicket estado) {
        return repository.findByVentaId(ventaId)
                .map(entity -> entity.toBuilder().estado(estado.name()).build())
                .collectList()
                .flatMapMany(repository::saveAll)
                .then();
    }
}

