package com.ticketseller.application.asiento;

import com.ticketseller.domain.exception.asiento.AsientoNotFoundException;
import com.ticketseller.domain.exception.zona.ZonaNotFoundException;
import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.repository.ZonaRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class AsignarAsientosAZonaUseCase {

    private final AsientoRepositoryPort asientoRepositoryPort;
    private final ZonaRepositoryPort zonaRepositoryPort;

    public Flux<Asiento> ejecutar(List<UUID> asientoIds, UUID zonaId) {
        return zonaRepositoryPort.buscarPorId(zonaId)
                .switchIfEmpty(Mono.error(new ZonaNotFoundException("Zona no encontrada: " + zonaId)))
                .flatMapMany(zona -> Flux.fromIterable(asientoIds)
                        .flatMap(id -> asientoRepositoryPort.buscarPorId(id)
                                .switchIfEmpty(Mono.error(new AsientoNotFoundException("Asiento no encontrado: " + id))))
                        .map(asiento -> asiento.toBuilder().zonaId(zonaId).build())
                        .collectList()
                        .flatMapMany(asientoRepositoryPort::guardarTodos));
    }
}
