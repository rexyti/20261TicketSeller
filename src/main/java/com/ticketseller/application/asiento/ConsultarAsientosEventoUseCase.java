package com.ticketseller.application.asiento;

import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

import java.util.UUID;

@RequiredArgsConstructor
public class ConsultarAsientosEventoUseCase {

    private final AsientoRepositoryPort asientoRepositoryPort;

    public Flux<Asiento> ejecutar(UUID eventoId) {
        return asientoRepositoryPort.buscarPorEventoId(eventoId);
    }
}
