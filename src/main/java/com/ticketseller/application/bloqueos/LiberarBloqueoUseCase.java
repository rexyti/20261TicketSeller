package com.ticketseller.application.bloqueos;

import com.ticketseller.domain.exception.bloqueos.BloqueoNoEncontradoException;
import com.ticketseller.domain.model.bloqueos.Bloqueo;
import com.ticketseller.domain.model.bloqueos.EstadoBloqueo;
import com.ticketseller.domain.repository.BloqueoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class LiberarBloqueoUseCase {

    private final BloqueoRepositoryPort bloqueoRepositoryPort;

    public Mono<Bloqueo> ejecutar(UUID bloqueoId) {
        return bloqueoRepositoryPort.buscarPorId(bloqueoId)
                .switchIfEmpty(Mono.error(new BloqueoNoEncontradoException(bloqueoId)))
                .flatMap(bloqueo -> {
                    Bloqueo bloqueoLiberado = bloqueo.toBuilder().estado(EstadoBloqueo.LIBERADO).build();
                    return bloqueoRepositoryPort.guardar(bloqueoLiberado);
                });
    }
}
