package com.ticketseller.application.evento;

import com.ticketseller.domain.exception.evento.EventoNotFoundException;
import com.ticketseller.domain.model.evento.Evento;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class IniciarEventoUseCase {

    private final EventoRepositoryPort eventoRepositoryPort;

    public Mono<Evento> ejecutar(UUID id) {
        return eventoRepositoryPort.buscarPorId(id)
                .switchIfEmpty(Mono.error(new EventoNotFoundException("Evento no encontrado")))
                .map(Evento::marcarEnProgreso)
                .flatMap(eventoRepositoryPort::guardar);
    }
}
