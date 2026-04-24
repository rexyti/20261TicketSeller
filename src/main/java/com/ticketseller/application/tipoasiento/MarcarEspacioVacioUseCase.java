package com.ticketseller.application.tipoasiento;

import com.ticketseller.domain.model.Asiento;
import com.ticketseller.domain.model.EstadoAsiento;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class MarcarEspacioVacioUseCase {

    private final AsientoRepositoryPort asientoRepositoryPort;

    public Mono<Asiento> ejecutar(UUID asientoId) {
        return asientoRepositoryPort.buscarPorId(asientoId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Asiento no encontrado")))
                .flatMap(this::marcarComoEnMantenimientoYGuardar);
    }

    private Mono<Asiento> marcarComoEnMantenimientoYGuardar(Asiento asiento) {
        Asiento actualizado = asiento.toBuilder()
                .estado(EstadoAsiento.EN_MANTENIMIENTO)
                .build();
        return asientoRepositoryPort.guardar(actualizado);
    }
}
