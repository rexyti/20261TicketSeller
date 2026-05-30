package com.ticketseller.application.evento;

import com.ticketseller.domain.model.evento.EstadoEvento;
import com.ticketseller.domain.model.evento.Evento;
import com.ticketseller.domain.model.evento.TipoEvento;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

@RequiredArgsConstructor
public class ListarEventosUseCase {

    private final EventoRepositoryPort eventoRepositoryPort;

    public Flux<Evento> ejecutar(EstadoEvento estado, TipoEvento tipo, LocalDate fechaInicioDesde, LocalDate fechaInicioHasta) {
        return eventoRepositoryPort.listarConFiltros(estado, tipo, fechaInicioDesde, fechaInicioHasta);
    }
}

