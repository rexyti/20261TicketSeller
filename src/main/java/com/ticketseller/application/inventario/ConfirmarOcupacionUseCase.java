package com.ticketseller.application.inventario;

import com.ticketseller.domain.exception.asiento.AsientoNoDisponibleException;
import com.ticketseller.domain.exception.asiento.HoldExpiradoException;
import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

public class ConfirmarOcupacionUseCase {

    private final AsientoRepositoryPort asientoRepositoryPort;

    public ConfirmarOcupacionUseCase(AsientoRepositoryPort asientoRepositoryPort) {
        this.asientoRepositoryPort = asientoRepositoryPort;
    }


    public Mono<Asiento> confirmar(UUID asientoId) {
        return asientoRepositoryPort.buscarPorId(asientoId)
                .switchIfEmpty(Mono.error(new AsientoNoDisponibleException("Asiento no encontrado: " + asientoId)))
                .flatMap(asiento -> {
                    if (!EstadoAsiento.RESERVADO.equals(asiento.getEstado())) {
                        return Mono.error(new AsientoNoDisponibleException(
                                "El asiento no está en estado RESERVADO"));
                    }
                    if (asiento.getExpiraEn() != null && asiento.getExpiraEn().isBefore(LocalDateTime.now())) {
                        return Mono.error(new HoldExpiradoException(
                                "El hold del asiento ha expirado"));
                    }
                    return asientoRepositoryPort.marcarOcupado(asientoId);
                });
    }

    public Mono<Asiento> liberar(UUID asientoId) {
        return asientoRepositoryPort.liberarHold(asientoId)
                .switchIfEmpty(Mono.error(new AsientoNoDisponibleException("Asiento no encontrado: " + asientoId)));
    }
}
