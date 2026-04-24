package com.ticketseller.application.evento;

import com.ticketseller.domain.model.EstadoEvento;
import com.ticketseller.domain.model.Evento;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
public class ListarEventosUseCase {

    private final EventoRepositoryPort eventoRepositoryPort;

    public Flux<Evento> ejecutar(EstadoEvento estado) {
        return estado == null
                ? eventoRepositoryPort.listarActivos()
                : eventoRepositoryPort.listarPorEstado(estado);
    }
}

