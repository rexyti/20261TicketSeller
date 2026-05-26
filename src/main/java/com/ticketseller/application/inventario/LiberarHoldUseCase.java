package com.ticketseller.application.inventario;

import com.ticketseller.domain.exception.asiento.AsientoNoDisponibleException;
import com.ticketseller.domain.model.asiento.AsientoHold;
import com.ticketseller.domain.repository.AsientoHoldRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class LiberarHoldUseCase {

    private final AsientoHoldRepositoryPort asientoHoldRepositoryPort;

    public Mono<AsientoHold> ejecutar(UUID asientoId, UUID eventoId) {
        return asientoHoldRepositoryPort.buscarActivoPorAsientoYEvento(asientoId, eventoId)
                .switchIfEmpty(Mono.error(new AsientoNoDisponibleException(
                        "No existe un hold activo para el asiento: " + asientoId)))
                .flatMap(hold -> {
                    hold.expirar();
                    return asientoHoldRepositoryPort.guardar(hold);
                });
    }
}
