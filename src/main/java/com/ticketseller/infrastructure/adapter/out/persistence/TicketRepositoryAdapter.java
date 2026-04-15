package com.ticketseller.infrastructure.adapter.out.persistence;

import com.ticketseller.domain.model.EstadoTicket;
import com.ticketseller.domain.model.Ticket;
import com.ticketseller.domain.port.out.TicketRepositoryPort;
import com.ticketseller.infrastructure.adapter.out.persistence.mapper.TicketPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TicketRepositoryAdapter implements TicketRepositoryPort {

    private final TicketR2dbcRepository ticketR2dbcRepository;
    private final TicketPersistenceMapper mapper;

    @Override
    public Mono<Ticket> guardar(Ticket ticket) {
        return ticketR2dbcRepository.save(mapper.toEntity(ticket))
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Ticket> guardarTodos(Flux<Ticket> tickets) {
        return tickets
                .map(mapper::toEntity)
                .flatMap(ticketR2dbcRepository::save)
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Ticket> buscarPorId(UUID id) {
        return ticketR2dbcRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Ticket> buscarPorVenta(UUID ventaId) {
        return ticketR2dbcRepository.findByVentaId(ventaId)
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Ticket> actualizarEstado(UUID id, EstadoTicket estado) {
        return ticketR2dbcRepository.findById(id)
                .flatMap(entity -> {
                    entity.setEstado(estado.name());
                    return ticketR2dbcRepository.save(entity);
                })
                .map(mapper::toDomain);
    }
}
