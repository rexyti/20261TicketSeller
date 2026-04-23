package com.ticketseller.application.evento;

import com.ticketseller.domain.exception.EventoNotFoundException;
import com.ticketseller.domain.model.Evento;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class CancelarEventoUseCase {

    private final EventoRepositoryPort eventoRepositoryPort;

    public Mono<Evento> ejecutar(UUID id, String motivo) {
        return eventoRepositoryPort.buscarPorId(id)
                .switchIfEmpty(Mono.error(new EventoNotFoundException("Evento no encontrado")))
                .map(evento -> evento.cancelarConMotivo(motivo))
                .flatMap(eventoRepositoryPort::guardar);
    }
}

