package com.ticketseller.infrastructure.adapter.out.persistence.checkout;

import com.ticketseller.domain.model.ticket.EstadoTicket;
import com.ticketseller.domain.model.ticket.Ticket;
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
    public Flux<Ticket> buscarPorVentaIds(Iterable<UUID> ventaIds) {
        return Flux.fromIterable(ventaIds)
                .collectList()
                .flatMapMany(ids -> ids.isEmpty() ? Flux.empty() : repository.findByVentaIdIn(ids))
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Ticket> buscarPorEvento(UUID eventoId) {
        return repository.findByEventoId(eventoId).map(mapper::toDomain);
    }

    @Override
    public Flux<Ticket> buscarPorEventoYEstados(UUID eventoId, Set<EstadoTicket> estados) {
        return repository.findByEventoIdAndEstadoIn(eventoId, estados.stream().map(Enum::name).toList())
                .map(mapper::toDomain);
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

