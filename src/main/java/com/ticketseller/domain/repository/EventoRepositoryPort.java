package com.ticketseller.domain.repository;

import com.ticketseller.domain.model.evento.EstadoEvento;
import com.ticketseller.domain.model.evento.Evento;
import com.ticketseller.domain.model.evento.TipoEvento;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public interface EventoRepositoryPort {

    Mono<Evento> guardar(Evento evento);

    Mono<Evento> buscarPorId(UUID id);

    Flux<Evento> listarTodos();

    Flux<Evento> listarPorEstado(EstadoEvento estado);

    Flux<Evento> listarConFiltros(EstadoEvento estado, TipoEvento tipo, LocalDate fechaInicioDesde, LocalDate fechaInicioHasta);

    Flux<Evento> buscarEventosSolapados(UUID recintoId, LocalDateTime fechaInicio, LocalDateTime fechaFin);
}

