package com.ticketseller.application.inventario;

import com.ticketseller.domain.exception.asiento.AsientoNoDisponibleException;
import com.ticketseller.domain.exception.asiento.AsientoNotFoundException;
import com.ticketseller.domain.exception.asiento.HoldExpiradoException;
import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import com.ticketseller.domain.repository.AsientoHoldRepositoryPort;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class ConfirmarOcupacionUseCase {

    private final AsientoRepositoryPort asientoRepositoryPort;
    private final AsientoHoldRepositoryPort asientoHoldRepositoryPort;

    public Mono<Asiento> ejecutar(UUID asientoId) {
        return asientoRepositoryPort.buscarPorId(asientoId)
                .switchIfEmpty(Mono.error(new AsientoNotFoundException("Asiento no encontrado: " + asientoId)))
                .filter(this::esReservado)
                .switchIfEmpty(Mono.error(new AsientoNoDisponibleException("El asiento no está en estado RESERVADO")))
                .flatMap(asiento -> asientoHoldRepositoryPort.buscarActivoPorAsientoId(asientoId)
                        .switchIfEmpty(Mono.error(new HoldExpiradoException("El hold del asiento ha expirado")))
                        .filter(this::holdNoExpirado)
                        .switchIfEmpty(Mono.error(new HoldExpiradoException("El hold del asiento ha expirado")))
                        .flatMap(hold -> asientoRepositoryPort.guardar(
                                asiento.toBuilder().estado(EstadoAsiento.VENDIDO).build())));
    }

    private boolean esReservado(Asiento asiento) {
        return EstadoAsiento.RESERVADO.equals(asiento.getEstado());
    }

    private boolean holdNoExpirado(com.ticketseller.domain.model.asiento.AsientoHold hold) {
        LocalDateTime ahora = LocalDateTime.now();
        return hold.getExpiraEn() == null || hold.getExpiraEn().isAfter(ahora);
    }
}
