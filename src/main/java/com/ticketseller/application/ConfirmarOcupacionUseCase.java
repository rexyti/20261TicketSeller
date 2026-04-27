package com.ticketseller.application;

import com.ticketseller.domain.repository.AsientoRepositoryPort;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class ConfirmarOcupacionUseCase {

    private final AsientoRepositoryPort asientoRepositoryPort;

    public ConfirmarOcupacionUseCase(AsientoRepositoryPort asientoRepositoryPort) {
        this.asientoRepositoryPort = asientoRepositoryPort;
    }

    public Mono<Void> ejecutar(UUID asientoId) {
        return asientoRepositoryPort.buscarPorId(asientoId)
                .flatMap(asiento -> {
                    if (!com.ticketseller.domain.model.EstadoAsiento.RESERVADO.equals(asiento.getEstado())) {
                        return Mono.error(new com.ticketseller.domain.exception.AsientoNoDisponibleException("El asiento no está reservado"));
                    }
                    if (asiento.getExpiraEn() != null && asiento.getExpiraEn().isBefore(java.time.LocalDateTime.now())) {
                        return Mono.error(new com.ticketseller.domain.exception.HoldExpiradoException("El hold del asiento ha expirado"));
                    }
                    return asientoRepositoryPort.marcarOcupado(asientoId);
                })
                .then();
    }
}
