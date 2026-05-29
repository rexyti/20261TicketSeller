package com.ticketseller.application.inventario;

import com.ticketseller.domain.exception.asiento.HoldExpiradoException;
import com.ticketseller.domain.model.asiento.AsientoHold;
import com.ticketseller.domain.repository.AsientoHoldRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class ConfirmarOcupacionUseCase {

    private final AsientoHoldRepositoryPort asientoHoldRepositoryPort;

    public Mono<AsientoHold> ejecutar(UUID asientoId, UUID eventoId) {
        return asientoHoldRepositoryPort.buscarActivoPorAsientoYEvento(asientoId, eventoId)
                .switchIfEmpty(Mono.error(new HoldExpiradoException("El hold del asiento ha expirado o no existe")))
                .flatMap(hold -> {
                    hold.vender();
                    return asientoHoldRepositoryPort.guardar(hold);
                });
    }
}
