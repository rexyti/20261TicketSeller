package com.ticketseller.application.inventario;

import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class LiberarHoldsVencidosUseCase {

    private final AsientoRepositoryPort asientoRepositoryPort;

    public Flux<Asiento> ejecutar(LocalDateTime ahora) {
        return asientoRepositoryPort.findHoldsVencidos(ahora)
                .flatMap(asiento -> asientoRepositoryPort.liberarHold(asiento.getId()));
    }
}
