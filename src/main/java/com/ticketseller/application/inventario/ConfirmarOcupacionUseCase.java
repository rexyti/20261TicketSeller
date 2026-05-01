package com.ticketseller.application.inventario;

import com.ticketseller.domain.exception.asiento.AsientoNoDisponibleException;
import com.ticketseller.domain.exception.asiento.HoldExpiradoException;
import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class ConfirmarOcupacionUseCase {

    private final AsientoRepositoryPort asientoRepositoryPort;

    public Mono<Asiento> confirmar(UUID asientoId) {
        return asientoRepositoryPort.buscarPorId(asientoId)
                .switchIfEmpty(Mono.error(new AsientoNoDisponibleException("Asiento no encontrado: " + asientoId)))
                .filter(asiento -> EstadoAsiento.RESERVADO.equals(asiento.getEstado()))
                .switchIfEmpty(Mono.error(new AsientoNoDisponibleException("El asiento no está en estado RESERVADO")))
                .filter(asiento -> asiento.getExpiraEn() == null || asiento.getExpiraEn().isAfter(LocalDateTime.now()))
                .switchIfEmpty(Mono.error(new HoldExpiradoException("El hold del asiento ha expirado")))
                .flatMap(asiento -> asientoRepositoryPort.marcarOcupado(asientoId));
    }

    public Mono<Asiento> liberar(UUID asientoId) {
        return asientoRepositoryPort.liberarHold(asientoId)
                .switchIfEmpty(Mono.error(new AsientoNoDisponibleException("Asiento no encontrado: " + asientoId)));
    }
}
