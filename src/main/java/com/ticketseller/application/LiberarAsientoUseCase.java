package com.ticketseller.application;

import com.ticketseller.domain.repository.AsientoRepositoryPort;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class LiberarAsientoUseCase {

    private final AsientoRepositoryPort asientoRepositoryPort;

    public LiberarAsientoUseCase(AsientoRepositoryPort asientoRepositoryPort) {
        this.asientoRepositoryPort = asientoRepositoryPort;
    }

    public Mono<Void> ejecutar(UUID asientoId) {
        return asientoRepositoryPort.liberarHold(asientoId).then();
    }
}
