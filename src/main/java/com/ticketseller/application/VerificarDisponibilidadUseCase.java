package com.ticketseller.application;

import com.ticketseller.domain.model.EstadoAsiento;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.infrastructure.adapter.in.rest.dto.DisponibilidadResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class VerificarDisponibilidadUseCase {

    private final AsientoRepositoryPort asientoRepositoryPort;

    public VerificarDisponibilidadUseCase(AsientoRepositoryPort asientoRepositoryPort) {
        this.asientoRepositoryPort = asientoRepositoryPort;
    }

    public Mono<DisponibilidadResponse> ejecutar(UUID asientoId) {
        return asientoRepositoryPort.buscarPorId(asientoId)
                .map(asiento -> {
                    boolean disponible = EstadoAsiento.DISPONIBLE.equals(asiento.getEstado());
                    String mensaje = disponible ? null : "ASIENTO NO DISPONIBLE";
                    return new DisponibilidadResponse(asientoId, disponible, mensaje);
                })
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Asiento no encontrado")));
    }
}
