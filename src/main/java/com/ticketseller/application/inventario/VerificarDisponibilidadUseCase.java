package com.ticketseller.application.inventario;

import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class VerificarDisponibilidadUseCase {

    private final AsientoRepositoryPort asientoRepositoryPort;

    public VerificarDisponibilidadUseCase(AsientoRepositoryPort asientoRepositoryPort) {
        this.asientoRepositoryPort = asientoRepositoryPort;
    }

    public Mono<Asiento> ejecutar(UUID asientoId) {
        return asientoRepositoryPort.buscarPorId(asientoId);
    }
}
