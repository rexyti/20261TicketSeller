package com.ticketseller.application.inventario;

import com.ticketseller.domain.exception.asiento.AsientoNoDisponibleException;
import com.ticketseller.domain.model.asiento.AsientoHold;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import com.ticketseller.domain.repository.AsientoHoldRepositoryPort;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class LiberarHoldUseCase {

    private final AsientoRepositoryPort asientoRepositoryPort;
    private final AsientoHoldRepositoryPort asientoHoldRepositoryPort;

    public Mono<AsientoHold> ejecutar(UUID asientoId) {
        return asientoHoldRepositoryPort.buscarActivoPorAsientoId(asientoId)
                .switchIfEmpty(Mono.error(new AsientoNoDisponibleException("No existe un hold activo para el asiento: " + asientoId)))
                .flatMap(hold -> asientoRepositoryPort.buscarPorId(asientoId)
                        .flatMap(asiento -> asientoRepositoryPort.guardar(
                                asiento.toBuilder().estado(EstadoAsiento.DISPONIBLE).build()))
                        .flatMap(saved -> {
                            hold.expirar();
                            return asientoHoldRepositoryPort.guardar(hold);
                        }));
    }
}
