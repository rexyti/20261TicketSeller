package com.ticketseller.application.compuerta;

import com.ticketseller.domain.exception.RecintoNotFoundException;
import com.ticketseller.domain.exception.ZonaNotFoundException;
import com.ticketseller.domain.model.Compuerta;
import com.ticketseller.domain.port.out.CompuertaRepositoryPort;
import com.ticketseller.domain.port.out.ZonaRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class AsignarCompuertaAZonaUseCase {

    private final CompuertaRepositoryPort compuertaRepositoryPort;
    private final ZonaRepositoryPort zonaRepositoryPort;

    public Mono<Compuerta> ejecutar(UUID compuertaId, UUID zonaId) {
        return compuertaRepositoryPort.buscarPorId(compuertaId)
                .switchIfEmpty(Mono.error(new RecintoNotFoundException("Compuerta no encontrada")))
                .flatMap(compuerta -> zonaRepositoryPort.buscarPorId(zonaId)
                        .switchIfEmpty(Mono.error(new ZonaNotFoundException("Zona no encontrada")))
                        .flatMap(zona -> compuertaRepositoryPort.guardar(buildCompuertaAsignada(zonaId, compuerta))));
    }

    private Compuerta buildCompuertaAsignada(UUID zonaId, Compuerta compuerta) {
        return compuerta.toBuilder()
                .zonaId(zonaId)
                .esGeneral(false)
                .build();
    }
}

