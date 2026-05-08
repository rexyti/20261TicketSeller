package com.ticketseller.application.bloqueos;

import com.ticketseller.domain.exception.bloqueos.BloqueoNoEncontradoException;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import com.ticketseller.domain.model.bloqueos.Bloqueo;
import com.ticketseller.domain.model.bloqueos.EstadoBloqueo;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.repository.BloqueoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class LiberarBloqueoUseCase {

    private final BloqueoRepositoryPort bloqueoRepositoryPort;
    private final AsientoRepositoryPort asientoRepositoryPort;

    public Mono<Bloqueo> ejecutar(UUID bloqueoId) {
        return bloqueoRepositoryPort.buscarPorId(bloqueoId)
                .switchIfEmpty(Mono.error(new BloqueoNoEncontradoException(bloqueoId)))
                .flatMap(this::liberarAsientoYBloqueo);
    }

    private Mono<Bloqueo> liberarAsientoYBloqueo(Bloqueo bloqueo) {
        Mono<Void> liberarAsiento = asientoRepositoryPort.buscarPorId(bloqueo.getAsientoId())
                .flatMap(asiento -> asientoRepositoryPort.guardar(
                        asiento.toBuilder().estado(EstadoAsiento.DISPONIBLE).build()))
                .then();

        Bloqueo bloqueoLiberado = bloqueo.toBuilder().estado(EstadoBloqueo.LIBERADO).build();
        return liberarAsiento.then(bloqueoRepositoryPort.guardar(bloqueoLiberado));
    }
}
