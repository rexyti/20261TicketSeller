package com.ticketseller.application;

import com.ticketseller.domain.exception.CortesiaNoEncontradaException;
import com.ticketseller.domain.model.Cortesia;
import com.ticketseller.domain.repository.CortesiaRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class GestionarCortesiaUseCase {

    private final CortesiaRepositoryPort cortesiaRepositoryPort;

    public Mono<Cortesia> editarDestinatario(UUID cortesiaId, String nuevoDestinatario) {
        return cortesiaRepositoryPort.buscarPorId(cortesiaId)
                .switchIfEmpty(Mono.error(new CortesiaNoEncontradaException(
                        "Cortesía no encontrada: " + cortesiaId)))
                .flatMap(cortesia -> {
                    Cortesia actualizada = cortesia.toBuilder()
                            .destinatario(nuevoDestinatario)
                            .build();
                    return cortesiaRepositoryPort.guardar(actualizada);
                });
    }
}
