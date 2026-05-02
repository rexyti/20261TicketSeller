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
public class GestionarBloqueoUseCase {

    private final BloqueoRepositoryPort bloqueoRepositoryPort;
    private final AsientoRepositoryPort asientoRepositoryPort;

    public Mono<Bloqueo> editarDestinatario(UUID bloqueoId, String nuevoDestinatario) {
        return bloqueoRepositoryPort.buscarPorId(bloqueoId)
                .switchIfEmpty(Mono.error(new BloqueoNoEncontradoException(bloqueoId)))
                .map(bloqueo -> bloqueo.toBuilder().destinatario(nuevoDestinatario).build())
                .flatMap(bloqueoRepositoryPort::guardar);
    }

    public Mono<Void> liberarBloqueo(UUID bloqueoId) {
        return bloqueoRepositoryPort.buscarPorId(bloqueoId)
                .switchIfEmpty(Mono.error(new BloqueoNoEncontradoException(bloqueoId)))
                .flatMap(bloqueo -> liberarAsientoYBloqueo(bloqueo));
    }

    private Mono<Void> liberarAsientoYBloqueo(Bloqueo bloqueo) {
        Mono<Void> liberarAsiento = asientoRepositoryPort.buscarPorId(bloqueo.getAsientoId())
                .flatMap(asiento -> asientoRepositoryPort.guardar(
                        asiento.toBuilder().estado(EstadoAsiento.DISPONIBLE).build()))
                .then();
        Mono<Void> actualizarBloqueo = bloqueoRepositoryPort.guardar(
                bloqueo.toBuilder().estado(EstadoBloqueo.LIBERADO).build()).then();
        return liberarAsiento.then(actualizarBloqueo);
    }
}
