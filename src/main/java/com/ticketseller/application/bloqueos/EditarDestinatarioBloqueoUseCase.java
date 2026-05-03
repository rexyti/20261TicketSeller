package com.ticketseller.application.bloqueos;

import com.ticketseller.domain.exception.bloqueos.BloqueoNoEncontradoException;
import com.ticketseller.domain.model.bloqueos.Bloqueo;
import com.ticketseller.domain.repository.BloqueoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class EditarDestinatarioBloqueoUseCase {

    private final BloqueoRepositoryPort bloqueoRepositoryPort;

    public Mono<Bloqueo> ejecutar(UUID bloqueoId, String nuevoDestinatario) {
        return bloqueoRepositoryPort.buscarPorId(bloqueoId)
                .switchIfEmpty(Mono.error(new BloqueoNoEncontradoException(bloqueoId)))
                .map(bloqueo -> bloqueo.toBuilder().destinatario(nuevoDestinatario).build())
                .flatMap(bloqueoRepositoryPort::guardar);
    }
}
