package com.ticketseller.application;

import com.ticketseller.domain.repository.AsientoRepositoryPort;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public class LiberarHoldsVencidosUseCase {

    private final AsientoRepositoryPort asientoRepositoryPort;

    public LiberarHoldsVencidosUseCase(AsientoRepositoryPort asientoRepositoryPort) {
        this.asientoRepositoryPort = asientoRepositoryPort;
    }

    public Mono<Void> ejecutar() {
        return asientoRepositoryPort.findHoldsVencidos(LocalDateTime.now())
                .flatMap(asiento -> asientoRepositoryPort.liberarHold(asiento.getId()))
                .then();
    }
}
