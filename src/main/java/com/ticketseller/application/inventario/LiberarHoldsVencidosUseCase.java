package com.ticketseller.application.inventario;

import com.ticketseller.domain.model.asiento.AsientoHold;
import com.ticketseller.domain.repository.AsientoHoldRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class LiberarHoldsVencidosUseCase {

    private final AsientoHoldRepositoryPort asientoHoldRepositoryPort;

    public Flux<AsientoHold> ejecutar(LocalDateTime ahora) {
        return asientoHoldRepositoryPort.buscarHoldsVencidos(ahora)
                .flatMap(hold -> {
                    hold.expirar();
                    return asientoHoldRepositoryPort.guardar(hold);
                });
    }
}
