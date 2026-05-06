package com.ticketseller.application.inventario;

import com.ticketseller.domain.model.asiento.AsientoHold;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import com.ticketseller.domain.model.asiento.EstadoHold;
import com.ticketseller.domain.repository.AsientoHoldRepositoryPort;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class LiberarHoldsVencidosUseCase {

    private final AsientoHoldRepositoryPort asientoHoldRepositoryPort;
    private final AsientoRepositoryPort asientoRepositoryPort;

    public Flux<AsientoHold> ejecutar(LocalDateTime ahora) {
        return asientoHoldRepositoryPort.buscarHoldsVencidos(ahora)
                .flatMap(hold -> asientoRepositoryPort.buscarPorId(hold.getAsientoId())
                        .flatMap(asiento -> asientoRepositoryPort.guardar(
                                asiento.toBuilder().estado(EstadoAsiento.DISPONIBLE).build()))
                        .then(asientoHoldRepositoryPort.guardar(
                                hold.toBuilder().estado(EstadoHold.EXPIRADO).build())));
    }
}
