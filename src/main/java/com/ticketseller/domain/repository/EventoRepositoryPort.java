package com.ticketseller.domain.repository;

import com.ticketseller.domain.model.EstadoEvento;
import com.ticketseller.domain.model.Evento;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

public interface EventoRepositoryPort {

    Mono<Evento> guardar(Evento evento);

    Mono<Evento> buscarPorId(UUID id);

    Flux<Evento> listarActivos();

    Flux<Evento> listarPorEstado(EstadoEvento estado);

    Flux<Evento> buscarEventosSolapados(UUID recintoId, LocalDateTime fechaInicio, LocalDateTime fechaFin);
}

