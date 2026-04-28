package com.ticketseller.infrastructure.adapter.out.persistence.evento;

import com.ticketseller.domain.model.evento.EstadoEvento;
import com.ticketseller.domain.model.evento.Evento;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import com.ticketseller.infrastructure.adapter.out.persistence.evento.mapper.EventoPersistenceMapper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class EventoRepositoryAdapter implements EventoRepositoryPort {

    private final EventoR2dbcRepository repository;
    private final EventoPersistenceMapper mapper;

    @Override
    public Mono<Evento> guardar(Evento evento) {
        return repository.save(mapper.toEntity(evento)).map(mapper::toDomain);
    }

    @Override
    public Mono<Evento> buscarPorId(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Flux<Evento> listarActivos() {
        return repository.findByEstado(EstadoEvento.ACTIVO.name()).map(mapper::toDomain);
    }

    @Override
    public Flux<Evento> listarPorEstado(EstadoEvento estado) {
        return repository.findByEstado(estado.name()).map(mapper::toDomain);
    }

    @Override
    public Flux<Evento> buscarEventosSolapados(UUID recintoId, LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return repository.buscarEventosSolapados(recintoId, fechaInicio, fechaFin).map(mapper::toDomain);
    }
}

