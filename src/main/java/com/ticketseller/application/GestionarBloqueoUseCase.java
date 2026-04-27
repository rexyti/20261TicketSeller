package com.ticketseller.application;

import com.ticketseller.domain.exception.BloqueoNoEncontradoException;
import com.ticketseller.domain.model.Bloqueo;
import com.ticketseller.domain.model.EstadoAsiento;
import com.ticketseller.domain.model.EstadoBloqueo;
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
                .switchIfEmpty(Mono.error(new BloqueoNoEncontradoException(
                        "Bloqueo no encontrado: " + bloqueoId)))
                .flatMap(bloqueo -> {
                    Bloqueo actualizado = bloqueo.toBuilder()
                            .destinatario(nuevoDestinatario)
                            .build();
                    return bloqueoRepositoryPort.guardar(actualizado);
                });
    }

    public Mono<Void> liberarBloqueo(UUID bloqueoId) {
        return bloqueoRepositoryPort.buscarPorId(bloqueoId)
                .switchIfEmpty(Mono.error(new BloqueoNoEncontradoException(
                        "Bloqueo no encontrado: " + bloqueoId)))
                .flatMap(bloqueo -> {
                    Bloqueo liberado = bloqueo.toBuilder()
                            .estado(EstadoBloqueo.LIBERADO)
                            .build();

                    return bloqueoRepositoryPort.guardar(liberado)
                            .then(asientoRepositoryPort.buscarPorId(bloqueo.getAsientoId())
                                    .flatMap(asiento -> {
                                        asiento.setEstado(EstadoAsiento.DISPONIBLE);
                                        return asientoRepositoryPort.guardar(asiento);
                                    }))
                            .then();
                });
    }
}
