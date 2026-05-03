package com.ticketseller.application.evento;

import com.ticketseller.domain.model.evento.EstadoEvento;
import com.ticketseller.domain.model.evento.Evento;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
public class ListarEventosUseCase {

    private final EventoRepositoryPort eventoRepositoryPort;

    public Flux<Evento> ejecutar(EstadoEvento estado) {
        return sinImportarElEstado(estado)
                ? eventoRepositoryPort.listarActivos()
                : eventoRepositoryPort.listarPorEstado(estado);
    }

    private boolean sinImportarElEstado(EstadoEvento estadoEvento){
        return estadoEvento == null;
    }
}

